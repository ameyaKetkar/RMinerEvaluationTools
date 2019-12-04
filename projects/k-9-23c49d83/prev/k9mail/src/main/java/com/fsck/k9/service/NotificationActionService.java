package com.fsck.k9.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mailstore.LocalMessage;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Service called by actions in notifications.
 * Provides a number of default actions to trigger.
 */
public class NotificationActionService extends CoreService {
    private final static String REPLY_ACTION = "com.fsck.k9.service.NotificationActionService.REPLY_ACTION";
    private final static String READ_ALL_ACTION = "com.fsck.k9.service.NotificationActionService.READ_ALL_ACTION";
    private final static String DELETE_ALL_ACTION = "com.fsck.k9.service.NotificationActionService.DELETE_ALL_ACTION";
    private final static String ARCHIVE_ALL_ACTION = "com.fsck.k9.service.NotificationActionService.ARCHIVE_ALL_ACTION";
    private final static String SPAM_ALL_ACTION = "com.fsck.k9.service.NotificationActionService.SPAM_ALL_ACTION";
    private final static String ACKNOWLEDGE_ACTION = "com.fsck.k9.service.NotificationActionService.ACKNOWLEDGE_ACTION";

    private final static String EXTRA_ACCOUNT = "account";
    private final static String EXTRA_MESSAGE = "message";
    private final static String EXTRA_MESSAGE_LIST = "messages";

    public static PendingIntent getReplyIntent(Context context, final Account account, final MessageReference ref) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE, ref);
        i.setAction(REPLY_ACTION);

        return PendingIntent.getService(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getReadAllMessagesIntent(Context context, final Account account, final Serializable refs) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE_LIST, refs);
        i.setAction(READ_ALL_ACTION);

        return PendingIntent.getService(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getAcknowledgeIntent(Context context, final Account account) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.setAction(ACKNOWLEDGE_ACTION);

        return PendingIntent.getService(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static Intent getDeleteAllMessagesIntent(Context context, final Account account, final Serializable refs) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE_LIST, refs);
        i.setAction(DELETE_ALL_ACTION);

        return i;
    }

    /**
     * Check if for the given parameters the ArchiveAllMessages intent is possible for Android Wear.
     * (No confirmation on the phone required and moving these messages to the spam-folder possible)<br/>
     * Since we can not show a toast like on the phone screen, we must not offer actions that can not be performed.
     * @see #getArchiveAllMessagesIntent(android.content.Context, com.fsck.k9.Account, java.io.Serializable)
     * @param context the context to get a {@link MessagingController}
     * @param account the account (must allow moving messages to allow true as a result)
     * @param messages the messages to move to the spam folder (must be synchronized to allow true as a result)
     * @return true if the ArchiveAllMessages intent is available for the given messages
     */
    public static boolean isArchiveAllMessagesWearAvaliable(Context context, final Account account, final LinkedList<LocalMessage> messages) {
        final MessagingController controller = MessagingController.getInstance(context);
        return (account.getArchiveFolderName() != null && !(account.getArchiveFolderName().equals(account.getSpamFolderName()) && K9.confirmSpam()) && isMovePossible(controller, account, account.getSentFolderName(), messages));
    }

    public static PendingIntent getArchiveAllMessagesIntent(Context context, final Account account, final Serializable refs) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE_LIST, refs);
        i.setAction(ARCHIVE_ALL_ACTION);

        return PendingIntent.getService(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    /**
     * Check if for the given parameters the SpamAllMessages intent is possible for Android Wear.
     * (No confirmation on the phone required and moving these messages to the spam-folder possible)<br/>
     * Since we can not show a toast like on the phone screen, we must not offer actions that can not be performed.
     * @see #getSpamAllMessagesIntent(android.content.Context, com.fsck.k9.Account, java.io.Serializable)
     * @param context the context to get a {@link MessagingController}
     * @param account the account (must allow moving messages to allow true as a result)
     * @param messages the messages to move to the spam folder (must be synchronized to allow true as a result)
     * @return true if the SpamAllMessages intent is available for the given messages
     */
    public static boolean isSpamAllMessagesWearAvaliable(Context context, final Account account, final LinkedList<LocalMessage> messages) {
        final MessagingController controller = MessagingController.getInstance(context);
        return (account.getSpamFolderName() != null && !K9.confirmSpam() && isMovePossible(controller, account, account.getSentFolderName(), messages));
    }

    public static PendingIntent getSpamAllMessagesIntent(Context context, final Account account, final Serializable refs) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE_LIST, refs);
        i.setAction(SPAM_ALL_ACTION);

        return PendingIntent.getService(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static boolean isMovePossible(MessagingController controller, Account account, String dstFolder, List<LocalMessage> messages) {
        if (!controller.isMoveCapable(account)) {
            return false;
        }
        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder)) {
            return false;
        }
        for(LocalMessage messageToMove : messages) {
            if (!controller.isMoveCapable(messageToMove)) {
                return false;
            }
        }
        return true;
    }
    @Override
    public int startService(Intent intent, int startId) {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "NotificationActionService started with startId = " + startId);
        final Preferences preferences = Preferences.getPreferences(this);
        final MessagingController controller = MessagingController.getInstance(getApplication());
        final Account account = preferences.getAccount(intent.getStringExtra(EXTRA_ACCOUNT));
        final String action = intent.getAction();

        if (account != null) {
            if (READ_ALL_ACTION.equals(action)) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "NotificationActionService marking messages as read");

                List<MessageReference> refs =
                        intent.getParcelableArrayListExtra(EXTRA_MESSAGE_LIST);
                for (MessageReference ref : refs) {
                    controller.setFlag(account, ref.getFolderName(), ref.getUid(), Flag.SEEN, true);
                }
            } else if (DELETE_ALL_ACTION.equals(action)) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "NotificationActionService deleting messages");

                List<MessageReference> refs =
                        intent.getParcelableArrayListExtra(EXTRA_MESSAGE_LIST);
                List<LocalMessage> messages = new ArrayList<LocalMessage>();

                for (MessageReference ref : refs) {
                    LocalMessage m = ref.restoreToLocalMessage(this);
                    if (m != null) {
                        messages.add(m);
                    }
                }

                controller.deleteMessages(messages, null);
            } else if (ARCHIVE_ALL_ACTION.equals(action)) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "NotificationActionService archiving messages");

                List<MessageReference> refs =
                        intent.getParcelableArrayListExtra(EXTRA_MESSAGE_LIST);
                List<LocalMessage> messages = new ArrayList<LocalMessage>();

                for (MessageReference ref : refs) {
                    LocalMessage m = ref.restoreToLocalMessage(this.getApplicationContext());
                    if (m != null) {
                        messages.add(m);
                    }
                }

                String dstFolder = account.getArchiveFolderName();
                if (dstFolder != null
                        && !(dstFolder.equals(account.getSpamFolderName()) && K9.confirmSpam())
                        && isMovePossible(controller, account, dstFolder, messages)) {
                    for(LocalMessage messageToMove : messages) {
                        if (!controller.isMoveCapable(messageToMove)) {
                            //Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
                            //toast.show();
                            continue;
                        }
                        String srcFolder = messageToMove.getFolder().getName();
                        controller.moveMessage(account, srcFolder, messageToMove, dstFolder, null);
                    }
                }
            } else if (SPAM_ALL_ACTION.equals(action)) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "NotificationActionService moving messages to spam");

                List<MessageReference> refs =
                        intent.getParcelableArrayListExtra(EXTRA_MESSAGE_LIST);
                List<LocalMessage> messages = new ArrayList<LocalMessage>();

                for (MessageReference ref : refs) {
                    LocalMessage m = ref.restoreToLocalMessage(this);
                    if (m != null) {
                        messages.add(m);
                    }
                }

                String dstFolder = account.getSpamFolderName();
                if (dstFolder != null
                        && !K9.confirmSpam()
                        && isMovePossible(controller, account, dstFolder, messages)) {
                    for(LocalMessage messageToMove : messages) {
                        String srcFolder = messageToMove.getFolder().getName();
                        controller.moveMessage(account, srcFolder, messageToMove, dstFolder, null);
                    }
                }
            } else if (REPLY_ACTION.equals(action)) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "NotificationActionService initiating reply");

                MessageReference ref = intent.getParcelableExtra(EXTRA_MESSAGE);
                LocalMessage message = ref.restoreToLocalMessage(this);
                if (message != null) {
                    Intent i = MessageCompose.getActionReplyIntent(this, message, false, null);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } else {
                    Log.i(K9.LOG_TAG, "Could not execute reply action.");
                }
            } else if (ACKNOWLEDGE_ACTION.equals(action)) {
                // nothing to do here, we just want to cancel the notification so the list
                // of unseen messages is reset
            }

            /* there's no point in keeping the notification after the user clicked on it */
            controller.notifyAccountCancel(this, account);
        } else {
            Log.w(K9.LOG_TAG, "Could not find account for notification action.");
        }

        return START_NOT_STICKY;
    }
}
