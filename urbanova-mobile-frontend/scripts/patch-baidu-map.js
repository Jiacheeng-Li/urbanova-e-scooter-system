const fs = require('fs');
const path = require('path');

const packageRoot = path.join(__dirname, '..');
const buildGradlePath = path.join(
  packageRoot,
  'node_modules',
  'react-native-baidu-map-yzg-wu',
  'android',
  'build.gradle'
);
const mapViewManagerPath = path.join(
  packageRoot,
  'node_modules',
  'react-native-baidu-map-yzg-wu',
  'android',
  'src',
  'main',
  'java',
  'org',
  'lovebing',
  'reactnative',
  'baidumap',
  'uimanager',
  'MapViewManager.java'
);
const mapListenerPath = path.join(
  packageRoot,
  'node_modules',
  'react-native-baidu-map-yzg-wu',
  'android',
  'src',
  'main',
  'java',
  'org',
  'lovebing',
  'reactnative',
  'baidumap',
  'listener',
  'MapListener.java'
);
const overlayMarkerPath = path.join(
  packageRoot,
  'node_modules',
  'react-native-baidu-map-yzg-wu',
  'android',
  'src',
  'main',
  'java',
  'org',
  'lovebing',
  'reactnative',
  'baidumap',
  'view',
  'OverlayMarker.java'
);

if (!fs.existsSync(buildGradlePath)) {
  console.warn('[patch-baidu-map] react-native-baidu-map-yzg-wu android build.gradle not found, skipping.');
  process.exit(0);
}

const source = `def safeExtGet(prop, fallback) {
    rootProject.ext.has(prop) ? rootProject.ext.get(prop) : fallback
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://maven.aliyun.com/nexus/content/groups/public/' }
        maven { url 'https://maven.aliyun.com/repository/jcenter' }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.8.2'
    }
}

apply plugin: 'com.android.library'

repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
    maven { url 'https://maven.aliyun.com/nexus/content/groups/public/' }
    maven { url 'https://maven.aliyun.com/repository/jcenter' }
}

android {
    namespace 'org.lovebing.reactnative.baidumap'
    compileSdkVersion safeExtGet('compileSdkVersion', 35)

    defaultConfig {
        minSdkVersion safeExtGet('minSdkVersion', 23)
        targetSdkVersion safeExtGet('targetSdkVersion', 35)
        versionCode 1
        versionName '1.3.13'
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a'
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly 'javax.annotation:javax.annotation-api:1.3.2'
    implementation 'com.facebook.react:react-android'
    api 'com.baidu.lbsyun:BaiduMapSDK_Location:9.3.0'
    api 'com.baidu.lbsyun:BaiduMapSDK_Search:7.4.0'
    api 'com.baidu.lbsyun:BaiduMapSDK_Map:7.4.0'
    api 'com.baidu.lbsyun:BaiduMapSDK_Util:7.4.0'
}
`;

fs.writeFileSync(buildGradlePath, source);
console.log('[patch-baidu-map] Patched react-native-baidu-map-yzg-wu Android Gradle config.');

if (fs.existsSync(mapViewManagerPath)) {
  const managerSource = fs.readFileSync(mapViewManagerPath, 'utf8');
  const patchedManagerSource = managerSource.replace(
    'if (child instanceof OverlayMarker) {\n                overlayMarkers.add((OverlayMarker) child);\n            }\n            ((OverlayView) child).removeFromMap(parent.getMap());',
    'if (child instanceof OverlayMarker) {\n                overlayMarkers.remove((OverlayMarker) child);\n            }\n            ((OverlayView) child).removeFromMap(parent.getMap());'
  );
  if (patchedManagerSource !== managerSource) {
    fs.writeFileSync(mapViewManagerPath, patchedManagerSource);
    console.log('[patch-baidu-map] Patched marker removal bookkeeping.');
  }
}

if (fs.existsSync(mapListenerPath)) {
  const listenerSource = fs.readFileSync(mapListenerPath, 'utf8');
  const start = listenerSource.indexOf('    @Override\n    public boolean onMarkerClick(Marker marker) {');
  const end = listenerSource.indexOf('\n    public void addMapStatusChangeListener', start);
  if (start !== -1 && end !== -1) {
    const markerClickHandler = `    @Override
    public boolean onMarkerClick(Marker marker) {
        try {
            WritableMap writableMap = Arguments.createMap();
            WritableMap position = Arguments.createMap();
            position.putDouble("latitude", marker.getPosition().latitude);
            position.putDouble("longitude", marker.getPosition().longitude);
            writableMap.putMap("position", position);
            writableMap.putString("title", marker.getTitle());
            OverlayMarker overlayMarker = MapViewManager.findOverlayMaker(marker);
            mapView.getMap().hideInfoWindow();
            if (overlayMarker != null) {
                InfoWindow infoWindow = overlayMarker.getInfoWindow(marker.getPosition());
                if (infoWindow != null) {
                    mapView.getMap().showInfoWindow(infoWindow);
                }
                reactContext
                        .getJSModule(RCTEventEmitter.class)
                        .receiveEvent(overlayMarker.getId(),
                                "topClick", writableMap);
            }
            sendEvent(mapView, "onMarkerClick", writableMap);
        } catch (Exception exception) {

        }
        return true;
    }
`;
    const patchedListenerSource = listenerSource.slice(0, start) + markerClickHandler + listenerSource.slice(end);
    if (patchedListenerSource !== listenerSource) {
      fs.writeFileSync(mapListenerPath, patchedListenerSource);
      console.log('[patch-baidu-map] Patched marker click event forwarding.');
    }
  }
}

if (fs.existsSync(overlayMarkerPath)) {
  const markerSource = fs.readFileSync(overlayMarkerPath, 'utf8');
  let patchedMarkerSource = markerSource;
  patchedMarkerSource = patchedMarkerSource.replace(
    '        if (title != null && title.length() > 0) {\n            if (titleInfoWindow == null) {',
    '        String displayTitle = title;\n        if (displayTitle != null && displayTitle.contains("|")) {\n            displayTitle = displayTitle.substring(displayTitle.indexOf("|") + 1);\n        }\n        if (displayTitle != null && displayTitle.length() > 0) {\n            if (titleInfoWindow == null) {'
  );
  patchedMarkerSource = patchedMarkerSource.replace(
    '                button.setText(title);',
    '                button.setText(displayTitle);'
  );
  patchedMarkerSource = patchedMarkerSource.replace(
    '        MarkerOptions option = new MarkerOptions()\n                .position(position)\n                .alpha(getAlpha())\n                .animateType(animateType)\n                .icon(getBitmapDescriptor());',
    '        MarkerOptions option = new MarkerOptions()\n                .position(position)\n                .alpha(getAlpha())\n                .animateType(animateType)\n                .icon(getBitmapDescriptor())\n                .title(title);'
  );
  if (patchedMarkerSource !== markerSource) {
    fs.writeFileSync(overlayMarkerPath, patchedMarkerSource);
    console.log('[patch-baidu-map] Patched marker title metadata forwarding.');
  }
}
