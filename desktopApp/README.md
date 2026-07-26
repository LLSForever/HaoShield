# Hǎo Shield for Windows

The same protected time as the phone, on the machine where the hours actually go.

It shares the session rules with Android (`:shared:data`) and the design system (`:shared:ui`),
and supplies its own persistence, blocking and object graph. Nothing in the shared modules is
Android-specific, and nothing in them had to change to be used from here.

## Running it

```
gradlew :desktopApp:run
```

Works with the JDK bundled in Android Studio — set `JAVA_HOME` to
`C:\Program Files\Android\Android Studio\jbr` if `java` is not on your PATH.

## What it does

- **Protected time, two ways.** An open session ends with a button. A Shield session ends only
  when the Shield is presented.
- **Making a Shield.** There is no camera here, so a printed card cannot be read. The desktop
  shows a code once and asks for it back; what you write down becomes the physical object. Keep
  the paper somewhere you have to get up to reach.
- **Blocking.** While a session runs, blocked apps are closed. Pick them from the curated groups
  or from whatever is running on your machine.
- **A way through.** Letting an app past costs a sentence about why and lasts fifteen minutes,
  after which the boundary returns. The reason is kept in the journal, against the app it was
  given for. It is a boundary rather than a wall, and this is the door.
- **The tray.** Closing the window leaves the app watching from the tray. Quitting is in the tray
  menu — this is meant to be friction, not a cage, but it has to be chosen.
- **Starting with Windows.** Offered from the installed app. It writes `HaoShield.cmd` into your
  Startup folder, which you can read and delete without this app's help.

## Two things to know before you rely on it

**Blocking is blunt.** A blocked app is *ended*, not asked to save first. That is fine for
launchers, chat apps and games, which keep their state on a server — which is why those are the
presets, and why browsers and anything document-shaped are deliberately not. Anything you add
yourself is your own judgement.

**Some things are never ended, whatever the blocklist says.** Anything under the Windows
directory, a list of processes Windows cannot lose, the machine's own defences, and this app
itself. Ending `csrss.exe` does not distract the machine, it takes it down.

## Packaging an installer

Needs a full JDK — `jpackage` is not in Android Studio's bundled runtime — and, for MSI, WiX
Toolset v3 on PATH.

```
gradlew :desktopApp:packageMsi -Phaoshield.packagingJdk="C:\path\to\jdk-21"
```

Use `packageDistributionForCurrentOS` for the default format, or `createDistributable` for a
plain app image with no installer. The property leaves the JDK used for everything else alone.

## Not there yet

No way to end a desktop session with the Shield your *phone* registered: the two apps keep their
own. Pairing them is the interesting next step, and the reason the token format is shared already.

No installer is built here yet — see packaging above, which needs a JDK this repo does not carry.
