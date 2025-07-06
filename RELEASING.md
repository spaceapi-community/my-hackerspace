# Releasing

Set variables:

    $ export VERSION=X.Y.Z
    $ export GPG_KEY=20EE002D778AE197EF7D0D2CB993FF98A90C9AB1  # Danilo

Update version numbers:

    $ vim app/build.gradle

Update changelog:

    $ vim CHANGELOG.md

Add the changelog to `metadata/en-US/changelogs/<version>.txt` as well.

Commit & tag:

    $ git commit -S${GPG_KEY} -m "Release v${VERSION}"
    $ git tag -s -u ${GPG_KEY} v${VERSION} -m "Version ${VERSION}"

Generate signed release artifacts:

    ./gradlew clean assembleRelease buildApksRelease

Copy release files:

    mkdir -p releases/${VERSION}/
    cp app/build/outputs/apk/release/app-release.apk releases/${VERSION}/MyHackerspace-${VERSION}.apk
    cp app/build/outputs/bundle/release/app-release.aab releases/${VERSION}/MyHackerspace-${VERSION}.aab
    cp app/build/outputs/apkset/release/app-release.apks releases/${VERSION}/MyHackerspace-${VERSION}.apks
    cp app/build/outputs/apk/release/output-metadata.json releases/${VERSION}/
    sed -i 's/app-release.apk/MyHackerspace-${VERSION}.apk/g' releases/${VERSION}/output-metadata.json

For the releases:

- GitHub: Upload the signed release APK file to GitHub releases
- Accrescent: Upload the signed release APKS file
- F-Droid: Will be updated automatically (might take a few days)
- Google Play: Upload the signed release AAB file to Play Console
