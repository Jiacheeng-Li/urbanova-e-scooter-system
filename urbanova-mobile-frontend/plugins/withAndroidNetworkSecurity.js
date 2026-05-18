const { AndroidConfig, withAndroidManifest, withDangerousMod } = require('@expo/config-plugins');
const fs = require('fs');
const path = require('path');

const NETWORK_SECURITY_XML = `<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
  <base-config cleartextTrafficPermitted="true">
    <trust-anchors>
      <certificates src="system" />
      <certificates src="user" />
    </trust-anchors>
  </base-config>
  <domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">${BackendIP}</domain>
  </domain-config>
</network-security-config>
`;

const withAndroidNetworkSecurity = (config) => {
  config = withAndroidManifest(config, (modConfig) => {
    const mainApplication = AndroidConfig.Manifest.getMainApplicationOrThrow(modConfig.modResults);
    mainApplication.$['android:usesCleartextTraffic'] = 'true';
    mainApplication.$['android:networkSecurityConfig'] = '@xml/network_security_config';

    const androidApiKey =
      process.env.EXPO_PUBLIC_BAIDU_MAP_ANDROID_AK ||
      config.extra?.baiduMap?.androidApiKey ||
      '';

    if (androidApiKey) {
      mainApplication['meta-data'] = mainApplication['meta-data'] || [];
      const metadata = mainApplication['meta-data'];
      const existing = metadata.find((item) => item.$?.['android:name'] === 'com.baidu.lbsapi.API_KEY');
      const entry = existing || { $: { 'android:name': 'com.baidu.lbsapi.API_KEY' } };
      entry.$['android:value'] = androidApiKey;
      if (!existing) {
        metadata.push(entry);
      }
    }

    return modConfig;
  });

  return withDangerousMod(config, [
    'android',
    (modConfig) => {
      const xmlDir = path.join(modConfig.modRequest.platformProjectRoot, 'app', 'src', 'main', 'res', 'xml');
      fs.mkdirSync(xmlDir, { recursive: true });
      fs.writeFileSync(path.join(xmlDir, 'network_security_config.xml'), NETWORK_SECURITY_XML);
      return modConfig;
    },
  ]);
};

module.exports = withAndroidNetworkSecurity;
