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

For the releases:

- GitHub: Upload the signed release APK file to GitHub releases
- Accrescent: Upload the signed release APKS file
- F-Droid: Will be updated automatically (might take a few days)
