#!/bin/bash

flutter clean
flutter channel stable
flutter doctor
flutter build ios --release
