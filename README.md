This is a implementation of Gerrit Secure Store extension point. It will use jasypt library to encrypt passwords and tokens used by Gerrit. It is just a naive implementation that stores password and salt seed used for encryption hardcoded in source code.

## Build

```
mvn package
```

## Installation

When starting new Gerrit site append `--secure-store` parameter to `init` program eg:
```
java -jar gerrit.war init --secure-store-lib $path/to/secure-store-jasypt.jar -d $site/path
```


To switch from other secure store provider, stop Gerrit then use `SwitchSecureStore` program eq:
```
java -jar gerrit.war SwitchSecureStore --new-secure-store-lib $patch/to/secure-store-jasypt.jar -d $stie/path
```
