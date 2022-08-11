from time import time
import flask
import cv2
from PIL import Image
import numpy as np
import time
import os
from skimage import io
from firebase_admin import credentials, initialize_app, storage

app = flask.Flask(__name__)


cred = credentials.Certificate(
    "D:\\Projects\\sample_flask\\edge-detector-de11d6a787ef.json")
initialize_app(
    cred, {'storageBucket': 'edge-detector.appspot.com'})


def get_file_names(filename):
    return "1" + str(int(time.time())) + filename, "2" + str(int(time.time())) + filename


def delete_file(filename):
    os.remove("D:\\Projects\\sample_flask\\" + filename)


def upload_file(bucket, filename, text):
    blob = bucket.blob(filename)
    blob.upload_from_filename(filename)
    blob.make_public()
    print(text, blob.public_url)


def save_image(filename, file_content):
    file = Image.fromarray(file_content)
    file.save(filename)


def upload_images_to_storage(normal_image, edge_detected_image, filename):
    filename1, filename2 = get_file_names(filename)
    save_image(filename1, normal_image)
    save_image(filename2, edge_detected_image)

    bucket = storage.bucket()
    upload_file(bucket, filename1, "Normal image URL - ")
    upload_file(bucket, filename2, "Edge detected image URL - ")
    delete_file(filename1)
    delete_file(filename2)


@app.route('/detect_edges_image_from_phone', methods=['POST'])
def detect_edges_of_image_from_phone():
    image_file = flask.request.files['image']
    print("\nReceived image File name : " + image_file.filename)
    normal_image = cv2.cvtColor(cv2.imdecode(np.frombuffer(
        image_file.read(), np.uint8), cv2.IMREAD_UNCHANGED), cv2.COLOR_BGR2RGB)
    edge_detected_image = cv2.Canny(normal_image, 100, 100)
    upload_images_to_storage(
        normal_image, edge_detected_image, image_file.filename)

    return "Image Uploaded Successfully"


@app.route('/detect_edges_image_from_url', methods=['POST'])
def detect_edges_of_image_from_url():
    url = flask.request.form.get('url')
    print(url)
    filename = url.split('/')[-1]
    normal_image = io.imread(url)
    edge_detected_image = cv2.Canny(normal_image, 100, 100)
    upload_images_to_storage(
        normal_image, edge_detected_image, filename)

    return "Image Uploaded Successfully"


if __name__ == '__main__':
    app.run(host='192.168.29.74', port=5000, debug=True)
