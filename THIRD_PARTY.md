# Third-party components — WebView2 edition

MusatPlay original code is covered by the repository MIT license. Dependencies retain their own licenses and notices.

- FlatLaf 3.7.2: Apache License 2.0. https://github.com/JFormDesigner/FlatLaf
- SwingWebView 1.12.0, Copyright Steve Hannah: MIT. https://github.com/webliteca/swingwebview
- Its native-loader code includes BSD-style licensed code from the University of Wisconsin-Madison, Glencoe Software and MX Telecom. Preserve the license notices in the dependency source and JAR.
- JNA / JNA Platform 5.19.1: LGPL 2.1 or later / Apache License 2.0, under its dual-license terms. https://github.com/java-native-access/jna
- Gson 2.11.0 and Error Prone annotations 2.27.0: Apache License 2.0. https://github.com/google/gson
- The bundled Eclipse Temurin Java runtime retains its legal directory and license files. https://adoptium.net/
- Microsoft Edge WebView2 Evergreen Runtime is a separately installed Microsoft component. Its browser distribution is not included in this package. https://developer.microsoft.com/microsoft-edge/webview2/
- The installer is built with WiX 3 and the Windows C# compiler. WiX UI resources retain their upstream license. https://github.com/wixtoolset/wix3

JCEF, JOGL, GlueGen and the former bundled Chromium native archive are absent from this edition. Dependency JARs remain unmodified, including their embedded license and notice files. Runtime dependency versions are recorded in pom.xml.

YouTube and YouTube Music are Google LLC trademarks; Microsoft Edge and WebView2 are Microsoft trademarks. MusatPlay is not affiliated with these companies.