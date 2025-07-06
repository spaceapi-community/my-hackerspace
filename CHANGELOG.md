# CHANGELOG

Possible tags:

- [info] An information not directly related to code changes
- [feature] A new feature or an improvement
- [bug] A bugfix
- [change] A change that's neither a feature nor a bugfix
- [i18n] Internationalization / translation


# Unreleased


# v2.3.1 (2025-07-07)

Just like 2.3.0, just the result of an error in the release process...


# v2.3.0 (2025-07-06)

- [feature] Allow setting app language through Android app settings ([#55])
- [change] Update build dependencies
- [change] Allow creating release artifacts through the command line ([#58])
- [change] Change TargetSDK to 36 (Android 16) ([#64])

Contributors to this version:

- Danilo Bargen (@dbrgn)
- @s3lph

Thanks!

[#55]: https://github.com/spaceapi-community/my-hackerspace/pull/55
[#58]: https://github.com/spaceapi-community/my-hackerspace/pull/58
[#64]: https://github.com/spaceapi-community/my-hackerspace/pull/64


# v2.2.0 (2025-01-20)

- [feature] Add support for SpaceAPI v15 ([#54])

Contributors to this version:

- @s3lph

Thanks!

[#54]: https://github.com/spaceapi-community/my-hackerspace/pull/54


# v2.1.3 (2024-07-21)

- [bug] Fix a parsing error for certain endpoints ([#50])
- [change] Upgrade dependencies, switch to Java 17, switch to Gradle 8, change
  TargetSDK to 34 ([#53])

Contributors to this version:

- Danilo Bargen (@dbrgn)

Thanks!

[#50]: https://github.com/spaceapi-community/my-hackerspace/pull/50
[#53]: https://github.com/spaceapi-community/my-hackerspace/pull/53


# v2.1.2 (2023-10-02)

- [feature] Show state "last change" timestamp as localized datetime ([#47])
- [bug] Use correct mastodon property ([#49])
- [bug] Fix network error message
- [change] Drop support for Android 5–7, require at least Android 8
- [change] Remove old autolinking-workaround for HTC devices ([#48])

Contributors to this version:

- cyroxx (@cyroxx)
- Danilo Bargen (@dbrgn)

Thanks!

[#47]: https://github.com/spaceapi-community/my-hackerspace/pull/47
[#48]: https://github.com/spaceapi-community/my-hackerspace/pull/48
[#49]: https://github.com/spaceapi-community/my-hackerspace/pull/49


## v2.1.1 (2023-04-27)

- [bug] Fix a bug when parsing v14 endpoints that contain a SpaceFED key
  without a "spacephone" field ([#44])
- [bug] Temporarily use directory from GitHub directly to speed up loading
  ([#45])

Contributors to this version:

- Danilo Bargen (@dbrgn)

Thanks!

[#44]: https://github.com/spaceapi-community/my-hackerspace/pull/44
[#45]: https://github.com/spaceapi-community/my-hackerspace/pull/45


## v2.1.0 (2023-04-16)

- [feature] Add information about the app to settings ([#18])
- [bug] Fix widget crashing on Android 12 ([#24])
- [bug] Export widget config activity to fix crashing OpenLauncher ([#33])
- [change] Reverse latitude and longitude displayed on the screen ([#37])
- [change] Improvements for widget ([#34])
- [change] Upgrade dependencies, change TargetSDK to 33 ([#42])

Contributors to this version:

- Danilo Bargen (@dbrgn)
- Nicco Kunzmann (@niccokunzmann)
- Adrian Pascu (@adipascu)
- Frieder Hannenheim (@FriederHannenheim)

Thanks!

[#18]: https://github.com/spaceapi-community/my-hackerspace/pull/18
[#24]: https://github.com/spaceapi-community/my-hackerspace/pull/24
[#33]: https://github.com/spaceapi-community/my-hackerspace/pull/33
[#34]: https://github.com/spaceapi-community/my-hackerspace/pull/34
[#37]: https://github.com/spaceapi-community/my-hackerspace/pull/37
[#42]: https://github.com/spaceapi-community/my-hackerspace/pull/42
[#44]: https://github.com/spaceapi-community/my-hackerspace/pull/44


## v2.0.2 (2022-08-07)

Unfortunately the app was pulled down from Google Play by Google due to
"developer inactivity". Due to the lack of e-mail forwarding, we never noticed
the warnings, so the app is now gone.

To fix this, we had to change the app ID. Additionally, there will be an F-Droid release.

Changes:

- [change] Update dependencies
- [change] Rename package to `io.spaceapi.community.myhackerspace`
- [info] Add F-Droid metadata


## v2.0.1 (2021-05-14)

- [bug] Fix refresh button ([#5][i5])

[i5]: https://github.com/spaceapi-community/my-hackerspace/pull/5


## v2.0.0 (2021-02-20)

- [info] App was re-released by the SpaceAPI project under a new package name ([#1][i1])
- [info] GitHub is now at https://github.com/spaceapi-community/my-hackerspace/
- [info] The app now requires at least Android 5 (API 21) ([#75][i75])
- [feature] Support for SpaceAPI v14 ([#85][i85])
- [feature] New app launcher icon ([#3][i3])
- [feature] More modern icons in app UI ([#74][i74])
- [bug] Don't save empty data in application state ([#64][i64])
- [change] Update all domains to spaceapi.io ([#65][i65], [#71][i71])
- [change] Switch to Java 8 ([#73][i73])
- [change] Remove MemorizingTrustManager ([#65][i65])
- [change] Upgrade dependencies ([#69][i69])
- [change] Switch to CircleCI ([#69][i69])
- [change] Add support for annotations ([#77][i77])
- [i18n] Improved translations

[i1]: https://github.com/spaceapi-community/my-hackerspace/pull/1
[i3]: https://github.com/spaceapi-community/my-hackerspace/pull/3
[i64]: https://github.com/fixme-lausanne/MyHackerspace/pull/64
[i65]: https://github.com/fixme-lausanne/MyHackerspace/pull/65
[i69]: https://github.com/fixme-lausanne/MyHackerspace/pull/69
[i71]: https://github.com/fixme-lausanne/MyHackerspace/pull/71
[i73]: https://github.com/fixme-lausanne/MyHackerspace/pull/73
[i74]: https://github.com/fixme-lausanne/MyHackerspace/pull/74
[i75]: https://github.com/fixme-lausanne/MyHackerspace/pull/75
[i77]: https://github.com/fixme-lausanne/MyHackerspace/pull/77
[i85]: https://github.com/fixme-lausanne/MyHackerspace/pull/85


## v1.8.3 (2017-01-XX)

- Change links from SpaceAPI.net to SpaceDirectory.org
- Display all webcams


## v1.8.2 (2016-07-02)

- Fix camera and stream url being displayed
- Fix twitter link to the new url format


## v1.8.1 (2016-05-06)

- Uses custom API directory end point (https://spaceapi.fixme.ch/directory.json)
- Allow editing of the API directory end point and the current hackerspace API
- Add Danish translation (thanks Mikkel)


## v1.8 (2016-04-14)

- Supports invalid SSL certificates
- Allow widget to be resized
- Add Dutch translation
- Fix http to https redirection
- General fixes


## v1.7.4.1 (2014-08-26)

- Fix crash when there's no error message


## v1.7.4 (2014-08-07)

- German translation (thanks to Lokke and Phervieux)
- Better hs list with alphabetical index
- Better errors messages
- Caching for http requests (images, hs directory)
- Add status message to the widget (thanks Fpletz)
- Fix bugs: widget updates, ignore ext fields, click from widget


## v1.7.3 (2013-10-25)

- Fix regression with widget custom open/close logo
- Fix order of hackerspaces with different cases


## v1.7.2 (2013-09-09)

- Better layout for sensors
- Support more fields for sensors (machines, names, properties)


## v1.7.1 (2013-09-06)

- Faster http requests (Use DefaultHttpClient instead of HttpURLConnection)


## v1.7 (2013-09-05)

- Full support of SpaceAPI 0.13, drops mixed api definition: hackerspaces must comply to the level they declare!
- Widget transparency preference added (by default transparency is deactivated)


## v1.6.1 (2013-06-04)

- French translation
- Fix the widget's image not updating
- Change to the new spaceapi url


## v1.6 (2013-01-02)

- Better layout in general
- Use Holo light theme for Android >=3
- Refresh the current hackerspace
- Default to 15mn for the Widget
- Settings button to change the widget interval
- Fix lat/lon link
- Fix crash when maps/email app not found


## v1.5.1 (2012-10-29)

- Bug fixes
- Add a spinner when loading image
- Faster download


## v1.5 (2012-05-19)

- Only download image if there is a change of state (better battery live and reduce network usage)


## v1.4 (2012-05-15)

- Add Cam and Stream links if present
- Link for adresses opening GMaps
- Sort Hackerspaces by name
- Accept untrusted SSL certificates
- Better error reporting
- BUGFIX: Theme shoud be correct on all devices/versions
- BUGFIX: Should work after reboot correctly


## v1.3 (2012-05-08)

- White theme by default (may break on samsung devices)
- Check if network is enabled
- Handle rotation correctly


## v1.2 (2012-05-06)


## v1.1 (2012-05-04)


## v1.0 (2012-04-29)

- Initial release
