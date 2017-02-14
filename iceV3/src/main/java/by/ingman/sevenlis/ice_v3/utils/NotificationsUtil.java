package by.ingman.sevenlis.ice_v3.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

import by.ingman.sevenlis.ice_v3.ErrorMessageActivity;
import by.ingman.sevenlis.ice_v3.MainActivity;
import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Answer;
import by.ingman.sevenlis.ice_v3.classes.Order;

public class NotificationsUtil {
    public static final int NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID = 10;
    public static final int NOTIF_UPDATE_PROGRESS_PRODUCTS_ID = 11;
    public static final int NOTIF_UPDATE_PROGRESS_DEBTS_ID = 12;
    public static final int NOTIF_UPDATE_PROGRESS_ORDERS_ID = 13;
    public static final int NOTIF_UPDATE_PROGRESS_ANSWERS_ID = 14;

    public static final int NOTIF_DATA_UPDATE_ERROR_ID = 20;
    public static final int NOTIF_ORDERS_ERROR_ID = 21;
    private static final int NOTIF_ORDER_SENT_ID = 3;
    private static final int NOTIF_ORDER_ACCEPT_ID = 4;

    private Context ctx;
    private NotificationManager notificationManager;

    public NotificationsUtil(Context context) {
        this.ctx = context;
    }

    public void showUpdateProgressNotification(int notificationId, String title, String message) {
        Notification.Builder builder = setupCommonNotification(title, message, false)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(0, 0, true)
                .setUsesChronometer(true);
        getNotificationManager().notify(notificationId, builder.build());
    }

    public void showUpdateProgressNotification(int notificationId) {
        String title = "Обновление...";
        String dataName;
        switch (notificationId) {
            case NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID:
                title = "Обновление контрагентов...";
                dataName = ctx.getString(R.string.notif_data_contragents);
                break;
            case NOTIF_UPDATE_PROGRESS_PRODUCTS_ID:
                title = "Обновление остатков...";
                dataName = ctx.getString(R.string.notif_data_products);
                break;
            case NOTIF_UPDATE_PROGRESS_DEBTS_ID:
                title = "Обновление задолженностей...";
                dataName = ctx.getString(R.string.notif_data_debts);
                break;
            case NOTIF_UPDATE_PROGRESS_ORDERS_ID:
                title = "Обновление заявок...";
                dataName = ctx.getString(R.string.notif_data_orders);
                break;
            case NOTIF_UPDATE_PROGRESS_ANSWERS_ID:
                title = "Получение ответов...";
                dataName = ctx.getString(R.string.notif_data_answers);
                break;
            default:
                dataName = "данные...";
                break;
        }
        String message = ctx.getResources().getString(R.string.notif_update_data_message, dataName);
        showUpdateProgressNotification(notificationId, title, message);
    }

    public void showUpdateCompleteNotification(int notificationId, String title, String message) {
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle().setBigContentTitle(title).bigText(message);
        Notification.Builder builder = setupCommonNotification(title, message, true)
                .setStyle(bigTextStyle)
                .setSmallIcon(R.drawable.ic_action_accept);
        getNotificationManager().notify(notificationId, builder.build());
    }

    public void showUpdateCompleteNotification(int notificationId) {
        String title = "Данные обновлены";
        String dataName;
        switch (notificationId) {
            case NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID:
                title = "Контрагенты обновлены";
                dataName = ctx.getString(R.string.notif_data_contragents_complete);
                break;
            case NOTIF_UPDATE_PROGRESS_PRODUCTS_ID:
                title = "Остатки обновлены";
                dataName = ctx.getString(R.string.notif_data_products_complete);
                break;
            case NOTIF_UPDATE_PROGRESS_DEBTS_ID:
                title = "Задолженности обновлены";
                dataName = ctx.getString(R.string.notif_data_debts_complete);
                break;
            case NOTIF_UPDATE_PROGRESS_ORDERS_ID:
                title = "Заявки обновлены";
                dataName = ctx.getString(R.string.notif_data_orders_complete);
                break;
            case NOTIF_UPDATE_PROGRESS_ANSWERS_ID:
                title = "Ответы обновлены";
                dataName = ctx.getString(R.string.notif_data_answers_complete);
                break;
            default:
                dataName = "данных";
        }
        String message = ctx.getResources().getString(R.string.notif_update_data_complete, dataName);
        showUpdateCompleteNotification(notificationId, title, message);
    }

    public void dismissNotification(int id) {
        getNotificationManager().cancel(id);
    }

    public void dismissAllUpdateNotifications() {
        dismissNotification(NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID);
        dismissNotification(NOTIF_UPDATE_PROGRESS_PRODUCTS_ID);
        dismissNotification(NOTIF_UPDATE_PROGRESS_DEBTS_ID);
        dismissNotification(NOTIF_UPDATE_PROGRESS_ORDERS_ID);
        dismissNotification(NOTIF_UPDATE_PROGRESS_ANSWERS_ID);
    }

    public void showOrderSentNotification(Order order) {
        String tag = order.orderUid;
        String title = ctx.getResources().getString(R.string.notif_request_success_title);
        String message = ctx.getResources().getString(R.string.notif_request_success_message, order.contragentName);

        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle().setBigContentTitle(title).bigText(message);

        Notification.Builder builder = setupCommonNotification(title, message, true)
                .setStyle(bigTextStyle)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done);

        getNotificationManager().notify(tag, NOTIF_ORDER_SENT_ID, builder.build());
    }

    public void showOrderAnswerNotification(Order order, Answer answer) {
        String tag = order.orderUid;
        String title = ctx.getResources().getString(R.string.notif_response_title, order.contragentName);
        String message = ctx.getResources().getString(R.string.notif_response_text, order.contragentName, answer.getDescription());

        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle().setBigContentTitle(title).bigText(message);

        Notification.Builder builder = setupCommonNotification(title, message, true)
                .setStyle(bigTextStyle)
                .setSmallIcon(R.drawable.ic_action_accept);

        getNotificationManager().notify(tag, NOTIF_ORDER_ACCEPT_ID, builder.build());
    }

    private Notification.Builder setupCommonNotification(String title, String message, boolean autoCancel) {
        return new Notification.Builder(ctx)
                    .setTicker(title)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setContentIntent(createMainIntent())
                    .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.mipmap.ice_pict))
                    .setAutoCancel(autoCancel);
    }

    private NotificationManager getNotificationManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    private PendingIntent createMainIntent() {
        return PendingIntent.getActivity(ctx, 0, new Intent(ctx, MainActivity.class), 0);
    }

    private PendingIntent createErrorIntent(Throwable th) {
        Intent intent = new Intent(ctx, ErrorMessageActivity.class);
        StringBuilder str = new StringBuilder(th.toString()).append("\n");

        for (StackTraceElement el : th.getStackTrace()) {
            str.append(el.toString()).append("\n");
        }
        intent.putExtra(ErrorMessageActivity.EXTRA_ERROR_MESSAGE, str.toString());

        return PendingIntent.getActivity(ctx, 1, intent, 0);
    }

    public void showErrorNotification(int notificationId, String title, String message, Throwable th) {
        Notification.Builder builder = setupCommonNotification(title, message, true)
                .setSmallIcon(android.R.drawable.stat_notify_error);

        if (th != null) {
            builder.setContentIntent(createErrorIntent(th));
        }

        getNotificationManager().notify(notificationId, builder.build());
    }
    public void dismissUpdateErrorNotifications() {
        getNotificationManager().cancel(NOTIF_DATA_UPDATE_ERROR_ID);
        getNotificationManager().cancel(NOTIF_ORDERS_ERROR_ID);
    }
}
