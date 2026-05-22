package com.emm.hello.newfeatures.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.emm.domain.validation.ValidationIssue
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import java.time.Instant

data class NewCardErrorUi(
    val title: String,
    val message: String? = null,
    val validationIssues: List<ValidationIssue> = emptyList(),
    val quotaResetAt: Instant? = null,
)

@Composable
fun NewCardErrorUi.localizedDescription(issueTextMapper: IssueTextMapper): String? {
    message?.let { return it }
    if (validationIssues.isEmpty()) return null
    return validationIssues
        .map { stringResource(issueTextMapper.map(it).textResId) }
        .joinToString(separator = "\n")
}
