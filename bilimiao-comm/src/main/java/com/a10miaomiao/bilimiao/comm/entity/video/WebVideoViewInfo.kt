package com.a10miaomiao.bilimiao.comm.entity.video

import kotlinx.serialization.Serializable

/**
 * https://api.bilibili.com/x/web-interface/view 接口返回中
 * 用于点赞率/投币率分析的精简结构。
 *
 * 该接口的 stat 字段中:
 *   - view: 播放量
 *   - like: 点赞数
 *   - coin: 投币数
 *   - favorite: 收藏数
 *   - share: 分享数
 *   - danmaku: 弹幕数
 *   - reply: 评论数
 */
@Serializable
data class WebVideoViewInfo(
    val aid: Long = 0L,
    val bvid: String = "",
    val title: String = "",
    val stat: WebVideoStat = WebVideoStat(),
)

@Serializable
data class WebVideoStat(
    val view: Long = 0L,
    val like: Long = 0L,
    val coin: Long = 0L,
    val favorite: Long = 0L,
    val share: Long = 0L,
    val danmaku: Long = 0L,
    val reply: Long = 0L,
)
