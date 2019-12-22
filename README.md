# Smart Speaker ESM 
## Overview
This is an voice-based ESM (Experience sampling method) app that was used to conduct an in-situ week-long study to understand opportune moments for proactive conversational interactions with smart speakers in the domestic context.

This ESM asked users the question, “Is now a good time to talk?” and it recorded reponses that participants answered in yes or no, and further inquired contextual reasoning for the provided answers.

## How To Use The App

1. Setting

<img width="249" alt="esmapp" src="https://user-images.githubusercontent.com/51148613/71318692-ce8ae880-24d7-11ea-9fd4-a9e10fbe9572.png">

- Check sound volume (by clicking "SOUND" button)
- Set operation time (by entering numbers in the start time and the end time blanks and click "ENTER" button)
- Take a picture of a room (by clicking "PICTURE" button)
- Adjust movement detection threshold (by clicking "MOVE" button, then you can see the number of different pixels on the screen. By checking the real-time number of different pixels, you can adjust the slider bar to change threshold.)
- Finish setting (by clickcing "END" button, then it will show "START" and "STOP" button)
2. Starting to collect data (by clicking "START" button)
3. Finishing to collect data (by clicking "STOP" button)

## How It Works
- It automatically (re)starts to collect ESM at the start time and pauses to collect ESM at the end time (until you click "STOP" button)
- During operation time, ESM prompts are randomly triggered or by movement detection at approximately the same interval (20 minutes). 
  - Movement is detected by finding differences between two consecutive real-time preview images by using the Android Camera Library.
  - Pictures are not saved for privacy issues, however, pixel-difference images (different pixels changed into black, while the same pixels changed into white) are saved to infer movement context.
- This app records the sound all the time (by making 30-seconds audio files), but remains neccesary files only (around the time that ESM prompted) to collect surrounding sounds before ESM prompts as well as to collect responses

## More...

At the study, this app was installed on Samsung Galaxy S7 smartphone.
We used DropSync to upload recording audio files in real time.
All created files (recording files, pixel-difference images) have their file name as time (milliseconds) forms, which can be used to link coincident data.
