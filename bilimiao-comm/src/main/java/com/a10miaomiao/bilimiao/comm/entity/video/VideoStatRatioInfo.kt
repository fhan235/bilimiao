package com.a10miaomiao.bilimiao.comm.entity.video

/**
 * 视频质量分析数据：点赞率、投币率，以及综合判定
 *
 * - likeRatio = like / view
 * - coinRatio = coin / view
 * - 当 likeRatio >= LIKE_RATIO_THRESHOLD 或 coinRatio >= likeRatio * COIN_RATIO_FACTOR 时
 *   视为优质视频
 *
 * 该模型仅用于 UI 展示，与播放器/收藏等业务无关。
 */
data class VideoStatRatioInfo(
    val plays: Long,
    val likes: Long,
    val coins: Long,
    val likeRatio: Float,
    val coinRatio: Float,
    val likeThreshold: Float,
    val coinThreshold: Float,
    val likePass: Boolean,
    val coinPass: Boolean,
    val isGood: Boolean,
) {
    companion object {
        // 点赞率阈值：5%
        const val LIKE_RATIO_THRESHOLD = 0.05f
        // 投币率系数：投币率阈值 = 点赞率 * 0.5
        const val COIN_RATIO_FACTOR = 0.5f

        fun evaluate(plays: Long, likes: Long, coins: Long): VideoStatRatioInfo {
            if (plays <= 0L) {
                return VideoStatRatioInfo(
                    plays = plays,
                    likes = likes,
                    coins = coins,
                    likeRatio = 0f,
                    coinRatio = 0f,
                    likeThreshold = LIKE_RATIO_THRESHOLD,
                    coinThreshold = 0f,
                    likePass = false,
                    coinPass = false,
                    isGood = false,
                )
            }
            val likeRatio = likes.toFloat() / plays.toFloat()
            val coinRatio = coins.toFloat() / plays.toFloat()
            val coinThreshold = likeRatio * COIN_RATIO_FACTOR
            val likePass = likeRatio >= LIKE_RATIO_THRESHOLD
            val coinPass = coinThreshold > 0f && coinRatio >= coinThreshold
            return VideoStatRatioInfo(
                plays = plays,
                likes = likes,
                coins = coins,
                likeRatio = likeRatio,
                coinRatio = coinRatio,
                likeThreshold = LIKE_RATIO_THRESHOLD,
                coinThreshold = coinThreshold,
                likePass = likePass,
                coinPass = coinPass,
                isGood = likePass || coinPass,
            )
        }
    }
}
