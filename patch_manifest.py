import re

with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    '<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />',
    '''<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />'''
)

service_block = '''        <service
            android:name=".ServerForegroundService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataSync" />'''

content = content.replace(
    '</application>',
    f'{service_block}\n    </application>'
)

with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
    f.write(content)
