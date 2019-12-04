package com.github.pockethub.ui.user;

import com.github.pockethub.ui.StyledText;
import com.github.pockethub.util.TypefaceUtils;

import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.IssuesPayload;

public enum EventType {
    CommitCommentEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatCommitComment(event, main, details);
            return TypefaceUtils.ICON_COMMENT;
        }
    },
    CreateEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatCreate(event, main, details);
            return TypefaceUtils.ICON_CREATE;
        }
    },
    DeleteEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatDelete(event, main, details);
            return TypefaceUtils.ICON_DELETE;
        }
    },
    DownloadEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatDownload(event, main, details);
            return TypefaceUtils.ICON_UPLOAD;
        }
    },
    FollowEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatFollow(event, main, details);
            return TypefaceUtils.ICON_FOLLOW;
        }
    },
    ForkEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatFork(event, main, details);
            return TypefaceUtils.ICON_FORK;
        }
    },
    GistEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatGist(event, main, details);
            return TypefaceUtils.ICON_GIST;
        }
    },
    GollumEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatWiki(event, main, details);
            return TypefaceUtils.ICON_WIKI;
        }
    },
    IssueCommentEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatIssueComment(event, main, details);
            return TypefaceUtils.ICON_ISSUE_COMMENT;
        }
    },
    IssuesEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatIssues(event, main, details);
            String action = ((IssuesPayload) event.getPayload()).getAction();
            String icon = null;
            if (IconAndViewTextManager.ISSUES_PAYLOAD_ACTION_OPENED.equals(action))
                icon = TypefaceUtils.ICON_ISSUE_OPEN;
            else if (IconAndViewTextManager.ISSUES_PAYLOAD_ACTION_REOPENED.equals(action))
                icon = TypefaceUtils.ICON_ISSUE_REOPEN;
            else if (IconAndViewTextManager.ISSUES_PAYLOAD_ACTION_CLOSED.equals(action))
                icon = TypefaceUtils.ICON_ISSUE_CLOSE;
            return icon;
        }
    },
    MemberEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatAddMember(event, main, details);
            return TypefaceUtils.ICON_ADD_MEMBER;
        }
    },
    PublicEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatPublic(event, main, details);
            return null;
        }
    },
    PullRequestEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatPullRequest(event, main, details);
            return TypefaceUtils.ICON_PULL_REQUEST;
        }
    },
    PullRequestReviewCommentEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatReviewComment(event, main, details);
            return TypefaceUtils.ICON_COMMENT;
        }
    },
    PushEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatPush(event, main, details);
            return TypefaceUtils.ICON_PUSH;
        }
    },
    TeamAddEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatTeamAdd(event, main, details);
            return TypefaceUtils.ICON_ADD_MEMBER;
        }
    },
    WatchEvent {
        @Override
        public String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details) {
            iconAndViewTextManager.formatWatch(event, main, details);
            return TypefaceUtils.ICON_STAR;
        }
    },
    ;

    public abstract String generateIconAndFormatStyledText(IconAndViewTextManager iconAndViewTextManager, Event event, StyledText main, StyledText details);
}
