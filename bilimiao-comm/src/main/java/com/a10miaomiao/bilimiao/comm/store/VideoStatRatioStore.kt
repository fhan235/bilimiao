package com.a10miaomiao.bilimiao.comm.store

import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.video.VideoStatRatioInfo
import com.a10miaomiao.bilimiao.comm.entity.video.WebVideoViewInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * 视频质量数据（点赞率/投币率）的内存缓存与请求调度器。
 *
 * - 单例：全局共享一份缓存，列表滚动/翻页时不会重复请求同一个视频
 * - 限流：[CONCURRENT_LIMIT] 个并发，每次请求后 [API_DELAY] 毫秒间隔，避免触发 B 站限流
 * - 缓存：[CACHE_TTL] 毫秒后过期；失败结果短期内不重试
 *
 * 用法：
 * ```
 * val ratioState = VideoStatRatioStore.observe(aid)
 * VideoStatRatioStore.requestIfNeeded(aid)
 * val ratio by ratioState.collectAsState()
 * ```
 */
object VideoStatRatioStore {

    /** 同时进行的请求数上限 */
    private const val CONCURRENT_LIMIT = 3

    /** 每次请求结束后的最小间隔（毫秒），用于平滑请求速率 */
    private const val API_DELAY_MS = 200L

    /** 缓存有效期：10 分钟 */
    private const val CACHE_TTL_MS = 10 * 60 * 1000L

    /** 失败缓存有效期：1 分钟（避免坏数据反复刷新） */
    private const val FAIL_CACHE_TTL_MS = 60 * 1000L

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data class Success(val info: VideoStatRatioInfo, val timestamp: Long) : State()
        data class Failure(val message: String, val timestamp: Long) : State()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(CONCURRENT_LIMIT)

    /** aid -> StateFlow<State> */
    private val states = ConcurrentHashMap<String, MutableStateFlow<State>>()

    /** aid -> 是否已经在调度中（避免重复入队） */
    private val inflight = ConcurrentHashMap<String, Boolean>()

    private val mutex = Mutex()

    /**
     * 订阅某个 aid 的状态。该方法不会触发请求，需要配合 [requestIfNeeded] 使用。
     */
    fun observe(aid: String): StateFlow<State> {
        return getOrCreateState(aid).asStateFlow()
    }

    /**
     * 请求一个视频的统计数据，若已缓存且有效则直接返回（不重新发请求）。
     */
    fun requestIfNeeded(aid: String) {
        if (aid.isBlank()) return
        scope.launch {
            mutex.withLock {
                val flow = getOrCreateState(aid)
                val current = flow.value
                val now = System.currentTimeMillis()
                // 已有有效缓存，无需重复请求
                when (current) {
                    is State.Success -> if (now - current.timestamp < CACHE_TTL_MS) return@withLock
                    is State.Failure -> if (now - current.timestamp < FAIL_CACHE_TTL_MS) return@withLock
                    State.Loading -> return@withLock
                    State.Idle -> {}
                }
                if (inflight[aid] == true) return@withLock
                inflight[aid] = true
                flow.value = State.Loading
            }
            // 释放锁后，进入限流队列
            try {
                semaphore.withPermit {
                    fetchAndUpdate(aid)
                    // 速率控制
                    delay(API_DELAY_MS)
                }
            } finally {
                inflight.remove(aid)
            }
        }
    }

    private suspend fun fetchAndUpdate(aid: String) {
        val flow = getOrCreateState(aid)
        try {
            // aid 参数实际上可能是 av 号或 bv 号；根据是否以 BV 开头自动判断 type
            val type = if (aid.startsWith("BV", ignoreCase = true)) "BV" else "AV"
            val res = BiliApiService.videoAPI.webView(aid, type = type)
                .awaitCall()
                .json<ResponseData<WebVideoViewInfo>>()
            if (res.isSuccess) {
                val data = res.requireData()
                val info = VideoStatRatioInfo.evaluate(
                    plays = data.stat.view,
                    likes = data.stat.like,
                    coins = data.stat.coin,
                )
                flow.value = State.Success(info, System.currentTimeMillis())
            } else {
                flow.value = State.Failure(res.message, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            flow.value = State.Failure(e.message ?: "request failed", System.currentTimeMillis())
        }
    }

    private fun getOrCreateState(aid: String): MutableStateFlow<State> {
        return states.getOrPut(aid) { MutableStateFlow(State.Idle) }
    }

    /** 测试 / 设置变化时清空缓存 */
    fun clear() {
        states.clear()
        inflight.clear()
    }
}
