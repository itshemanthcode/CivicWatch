package com.example.claudeapp.utils

import android.content.Context
import android.content.Intent
import com.example.claudeapp.data.model.Issue

fun shareIssue(context: Context, issue: Issue) {
    val shareText = buildString {
        append(context.getString(com.example.claudeapp.R.string.share_title) + "\n\n")
        append(context.getString(com.example.claudeapp.R.string.share_category, issue.category) + "\n")
        append(context.getString(com.example.claudeapp.R.string.share_status, issue.status) + "\n")
        if (issue.description.isNotEmpty()) {
            append(context.getString(com.example.claudeapp.R.string.share_description, issue.description) + "\n")
        }
        if (issue.location.address.isNotEmpty()) {
            append(context.getString(com.example.claudeapp.R.string.share_location, issue.location.address) + "\n\n")
        } else {
             append(context.getString(com.example.claudeapp.R.string.share_location, "${issue.location.city}, ${issue.location.state}") + "\n\n")
        }
        
        if (issue.images.isNotEmpty()) {
            append(context.getString(com.example.claudeapp.R.string.share_image, issue.images.first()) + "\n\n")
        }
        append(context.getString(com.example.claudeapp.R.string.share_footer))
    }

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, context.getString(com.example.claudeapp.R.string.share_chooser_title))
    context.startActivity(shareIntent)
}
