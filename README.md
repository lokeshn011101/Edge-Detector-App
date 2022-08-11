# Edge-Detector-App

## Steps for running the app

The `master` branch contains the app. And the `sdk` branch contains the app as a library.
For running the app, `clone` the master branch and run it normally.

## Steps for running the server

### Prerequisites
1. Create a firebase project and enable `Storage`.
2. Go to [cloud.console.google.com](https://console.cloud.google.com/projectselector2/iam-admin/serviceaccounts) and select the firebase project.
3. Click on the value in the `Actions` column in the 1st row.
4. Select `Manage Keys`.
5. Click `Add Key` and select `Create a new key`.
6. Copy the downloaded json file to `</path_to_app>/server/`
7. Open `app.py` and paste the complete path of the creds json file in line 14.

### Run the server
1. `cd server/`
2. `python -m venv env`
3. `.\env\Scripts\activate`
4. `pip install requirements.txt`
5. `python app.py`
