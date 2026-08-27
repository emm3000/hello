package com.emm.hello.newfeatures.hoy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.emm.domain.study.DashboardStats
import com.emm.hello.R
import com.emm.hello.core.ui.HStatCard

@Composable
fun HoyStatsSection(
    stats: DashboardStats,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HStatCard(
                value = "${stats.cardsStudiedToday}",
                label = stringResource(R.string.hoy_stat_studied_today),
                modifier = Modifier
                    .weight(1f),
            )
            HStatCard(
                value = "${stats.cardsDueToday}",
                label = stringResource(R.string.hoy_stat_due_today),
                modifier = Modifier
                    .weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HStatCard(
                value = "${stats.currentStreak}",
                label = stringResource(R.string.hoy_stat_day_streak),
                modifier = Modifier
                    .weight(1f),
            )
            HStatCard(
                value = "${stats.cardsDueThisWeek}",
                label = stringResource(R.string.hoy_stat_due_this_week),
                modifier = Modifier
                    .weight(1f),
            )
        }
    }
}
