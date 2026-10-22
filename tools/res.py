"""Writes the small hand-made resources: theme, colours, launcher icon and the notification icon.

python tools/res.py
"""
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"

# A dusty Bordeaux bottle with a red wax capsule and a cream label, standing in a candlelit arch.
BOTTLE = """    <path android:fillColor="#40000000" android:pathData="M40,83h28a14,2.4 0,0 1,-28 0z" />
    <path android:fillColor="#2E4A2B" android:pathData="M45,83V52C45,45 50.5,44 50.5,39V25h7v14c0,5 5.5,6 5.5,13v31z" />
    <path android:fillColor="#20341E" android:pathData="M57.5,25v14c0,5 5.5,6 5.5,13v31h-5V52c0,-5 -2.5,-7 -2.5,-13V25z" />
    <path android:fillColor="#33FFFFFF" android:pathData="M47.5,52h2v28h-2z" />
    <path android:fillColor="#55B9AE9E" android:pathData="M45,52C45,45 50.5,44 50.5,39h7c0,5 5.5,6 5.5,13z" />
    <path android:fillColor="#A3262A" android:pathData="M50,21h8v9h-8z" />
    <path android:fillColor="#C24045" android:pathData="M50,21h3v9h-3z" />
    <path android:fillColor="#EADCBE" android:pathData="M46.5,60h15v15h-15z" />
    <path android:fillColor="#3A2A1C" android:pathData="M49,65h10v1.2h-10z M50,69h8v1h-8z" />"""

FILES = {
    "values/themes.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Cellar" parent="Theme.AppCompat.NoActionBar">
        <item name="android:windowBackground">@color/window</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="colorPrimary">@color/flame</item>
        <item name="colorAccent">@color/flame</item>
    </style>
</resources>
""",
    "values/colors.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="window">#FF17120F</color>
    <color name="flame">#FFE9A447</color>
</resources>
""",
    "mipmap-anydpi-v26/ic_launcher.xml": """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_monochrome" />
</adaptive-icon>
""",
    "drawable/ic_launcher_background.xml": """<?xml version="1.0" encoding="utf-8"?>
<!-- Dark stone, a brick arch and the warm light of a candle behind the bottle. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#17120F" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#5A4636" android:pathData="M22,92V44a32,32 0,0 1,64 0v48h-6V44a26,26 0,0 0,-52 0v48z" />
    <path android:fillColor="#2A1F18" android:pathData="M28,92V44a26,26 0,0 1,52 0v48z" />
    <path android:fillColor="#3A2A16" android:pathData="M54,50m-22,0a22,22 0,1 1,44 0a22,22 0,1 1,-44 0" />
    <path android:fillColor="#4A3418" android:pathData="M54,50m-14,0a14,14 0,1 1,28 0a14,14 0,1 1,-28 0" />
    <path android:fillColor="#3F2A19" android:pathData="M22,84h64v8h-64z" />
</vector>
""",
    "drawable/ic_launcher_foreground.xml": f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
{BOTTLE}
</vector>
""",
    "drawable/ic_monochrome.xml": """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:pathData="M45,83V52C45,45 50.5,44 50.5,39V30h-0.5v-9h8v9h-0.5v9c0,5 5.5,6 5.5,13v31z" />
</vector>
""",
    "drawable/ic_notify.xml": """<?xml version="1.0" encoding="utf-8"?>
<!-- The bottle, for the status bar. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M8.5,22V11.5C8.5,9.5 10.5,9 10.5,7.5V4.5h-0.3V2h3.6v2.5h-0.3v3c0,1.5 2,2 2,4V22z" />
</vector>
""",
}


def main():
    for name, text in FILES.items():
        f = RES / name
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(text, encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
