# Coins3 API
How to include the API with Maven: 
```xml
<repositories>
    .....
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
    .....
</repositories>
<dependencies>
    .....
    <dependency>
        <groupId>com.github.beelzebu</groupId>
        <artifactId>coins3-api</artifactId>
        <version>3.0</version>
        <scope>provided</scope>
    </dependency>
    .....
</dependencies>
```

How to include the API with Gradle:
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly "com.github.beelzebu:coins3-api:3.0"
}
```