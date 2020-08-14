[![](https://jitpack.io/v/Haransis/WaveFormPlayer.svg)](https://jitpack.io/#Haransis/WaveFormPlayer)
<p align="center">
  <img src="https://github.com/haransis/waveformplayer/blob/assets/Player.png"><br><br>
</p>

# WaveFormPlayer
An Android library to create visualization of a given sound. It also supports a wav recorder with sound wave visualization. This is a very basic implementation, use at your own risk.

Note that the library does not build the amplitudes by itself when reading from a file.

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
       implementation 'com.github.Haransis:WaveFormPlayer:1.2.0'
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
    <version>1.2.0</version>
</dependency>
```

## Usage
### Player
1. Add the player view in your layout :
```
<fr.haran.soundwave.ui.PlayerView
    android:id="@+id/player_view"
    app:fontName="ibm_plex_mono_regular.ttf"
    app:title="Music Title"
    app:waveDb="true"
    app:mainColor="@android:color/black"
    app:secondaryColor="@android:color/darker_gray"
    ... />
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
The amplitudes are an array of double inside [-1.0; 1.0].

4. Do not forget to destroy the player inside the OnDestroy method of your Activity.
```
playerController.destroyPlayer()
```

### Recorder
The recorder ressembles to the player a lot but here is a quick tutorial.
1. Add the recorder view in your layout
```
<fr.haran.soundwave.ui.RecPlayerView
        android:id="@+id/rec_player_view"
        app:rec_color="@android:color/black"
        app:rec_playedColor="@android:color/darker_gray" 
        ... />
```

2. In your Activity, create a custom controller implementing the RecorderController interface or use the default one (You need to reference the view, and the path where the files will be created) :
```
recorderController = DefaultRecorderController(findViewById(R.id.rec_player_view), applicationContext.externalCacheDir?.absolutePath?)
```

3. Set a listener and prepare the controller
```
recorderController.setRecorderListener(validate = {...})
recorderController.prepareRecorder()
```
Using the default listener is perfectly fine but you should at least use the validate function to use the files.

4. Do not forget to destroy the recorder inside the OnDestroy method of your Activity.
```
recorderController.destroyPlayer()
```
For a more detailed implementation, please have a look at the example app in the repository.

## Building the amplitudes
As mentionned before, for now this library does not implement the calculation of the amplitudes so you should implement this by yourself. For my use case, a static array was enough.
Moreover the formats the recorder and the player uses are different. This will be solved soon.
