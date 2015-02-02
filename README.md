This is a implementation of Gerrit Secure Store extension point. It will use jasypt library to encrypt passwords and tokens used by Gerrit. It is just a naive implementation that stores password and salt seed used for encryption hardcoded in source code.

## Build

```
mvn package
```

## Installation

When starting new Gerrit site append `--secure-store` parameter to `init` program eg:
```
java -cp gerrit.war:$path/to/jasypt-1.9.2.jar Main init --secure-store-lib $path/to/secure-store-jasypt.jar -d $site/path
```
Then, before staring Gerrit put jasypt jar into $site/path/lib directory.

To switch from other secure store provider, stop Gerrit then use `SwitchSecureStore` program eq:
```
java -cp gerrit.war:$path/to/jasypt-1.9.2.jar Main SwitchSecureStore --new-secure-store-lib $patch/to/secure-store-jasypt.jar -d $stie/path
```
afterwards copy jasypt jar file into $site/path/lib directory and start Gerrit.