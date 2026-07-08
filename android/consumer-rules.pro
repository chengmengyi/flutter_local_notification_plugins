-keep class android.support.v4.media.session.MediaSessionCompat { *; }
-keep class android.support.v4.media.session.MediaSessionCompat$Token { *; }

-keepclassmembers class android.support.v4.media.session.MediaSessionCompat {
    public static final int FLAG_HANDLES_MEDIA_BUTTONS;
    public static final int FLAG_HANDLES_TRANSPORT_CONTROLS;
    public <init>(android.content.Context, java.lang.String);
    public void setFlags(int);
    public void setActive(boolean);
    public void setPlaybackState(android.support.v4.media.session.PlaybackStateCompat);
    public android.support.v4.media.session.MediaSessionCompat$Token getSessionToken();
}

-keep class android.support.v4.media.session.PlaybackStateCompat { *; }
-keep class android.support.v4.media.session.PlaybackStateCompat$Builder { *; }

-keepclassmembers class android.support.v4.media.session.PlaybackStateCompat$Builder {
    public <init>();
    public android.support.v4.media.session.PlaybackStateCompat$Builder setState(int,long,float);
    public android.support.v4.media.session.PlaybackStateCompat build();
}

-keep class androidx.media.app.NotificationCompat$MediaStyle { *; }
-keepclassmembers class androidx.media.app.NotificationCompat$MediaStyle {
    public <init>();
    public androidx.media.app.NotificationCompat$MediaStyle setMediaSession(android.support.v4.media.session.MediaSessionCompat$Token);
    android.support.v4.media.session.MediaSessionCompat$Token mToken;
}
