# WhereAmI
An application that allows you to track the current location of the device on a google map with the exact position information provided

## About
Android project is using Google Maps-based APIs for real-time experiences with the latest Maps, Routes, and Places features from Google Maps Platform. Application is also using external storage for saving captured images in application. Sensor for playing background audio when changing marker on a map is also used in app.

## Features
The android app lets you:
 - Check your current position on a map 
 - Recentering on a map
 - Reviewing information about current position latitude, longitude, address etc. 
 - Changing location marker with background sound  
 - capturing images of current location and saving it to external storage 
 - Getting notification for saved image which leads to gallery where image is saved 
 
## Tech-stack
The project seeks to use recommended practices and libraries in Android development:
- MVVM architecture (Viewmodel + LiveData)
- Data Binding
- View Binding
- Hilt dependency injection
- Geocoder
- ...

## Screenshots
![image](https://user-images.githubusercontent.com/75457058/128776397-665e1bb1-e143-458e-87a1-cf962b0967eb.png)
![image](https://user-images.githubusercontent.com/75457058/128776416-41697ed7-18cb-43c9-b2b5-df0041e35d4b.png)

## Permissions
WhereAmI requires the following permissions in AndroidManifest.xml:
- permissions for writing and reading external storage 
- camera permission
- fine location access permission 
- internet permission for fetching data from google API 

## Setup
1. Clone the repository
```
https://github.com/kovaccc/WhereAmI.git
```
2. Open the project with your IDE/Code Editor
3. Run it on simulator or real Android device
