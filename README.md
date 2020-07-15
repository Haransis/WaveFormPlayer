[![](https://jitpack.io/v/Haransis/WaveFormPlayer.svg)](https://jitpack.io/#Haransis/WaveFormPlayer)
<p align="center">
  <img src="https://github.com/haransis/waveformplayer/blob/assets/Player.png"><br><br>
</p>

# WaveFormPlayer
An Android library to create visualization of a given sound. This is a very basic implementation, use at your own risk.

Note that the library does not build the amplitudes by itself.

## Installation
### Gradle
1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```
    allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
2. Add the dependency in Gradle
```
    dependencies {
       implementation 'com.github.Haran:WaveFormPlayer:1.0.1'
    }
```

### Maven
1. Add the JitPack repository to your build file
```
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>
```
2. Add the dependency
```
<dependency>
    <groupId>com.github.Haransis</groupId>
    <artifactId>WaveFormPlayer</artifactId>
    <version>Tag</version>
</dependency>
```

## Usage
1. Add the player view in your layout :
```
<fr.haran.soundwave.ui.PlayerView
    android:id="@+id/player_view"
    android:layout_width="match_parent"
    android:layout_height="200dp"
    app:fontName="ibm_plex_mono_regular.ttf"
    app:title="Music Title"
    app:waveDb="true"
    app:mainColor="@android:color/black"
    app:secondaryColor="@android:color/darker_gray"/>
```

2. In your Activity, create a custom controller implementing the PlayerController interface or use the default one :
```
playerController = DefaultPlayerController(
            findViewById<PlayerView>(R.id.player_view)
        )
```
You need to set a Listener to the default controller. Using an empty one will use the default ones.
```
playerController.setListener(play = {...},
                             complete = {...},
                             prepare = {...},
                             pause = {...},
                             complete = {...},
                             progress = {duration, time -> ...})
```

3. Add a ressource with the associated amplitudes to your controller
- Local file
```
val uri = Uri.parse("android.resource://$packageName/raw/france")
try {
    playerController.addAudioFileUri(applicationContext, amplitudes)
} catch (e: IOException) {
    e.printStackTrace()
}
```
- From an URL
```
view.addAudioUrl(url,amplitudes)
```
Note : you need to add the Internet permission in your manifest

For a more detailed use case application, please have a look at the example app in the repository.

## Building the amplitudes
As mentionned before, for now this library does not implement the calculation of the amplitudes so you should implement this by yourself. For my use case, a static array was enough.