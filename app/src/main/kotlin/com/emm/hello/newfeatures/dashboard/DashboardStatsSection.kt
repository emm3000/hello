package com.emm.hello.newfeatures.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emm.domain.study.DashboardStats
import com.emm.hello.core.ui.HStatCard

@Composable
fun DashboardStatsSection(
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
                label = "Studied today",
                modifier = Modifier
                    .weight(1f),
            )
            HStatCard(
                value = "${stats.cardsDueToday}",
                label = "Due today",
                modifier = Modifier
                    .weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HStatCard(
                value = "${stats.currentStreak}",
                label = "Day streak",
                modifier = Modifier
                    .weight(1f),
            )
            HStatCard(
                value = "${stats.cardsDueThisWeek}",
                label = "Due this week",
                modifier = Modifier
                    .weight(1f),
            )
        }
    }
}
