from flask import Flask, request
import sqlite3
import requests
import cv2
import numpy as np
import cpickle
import base64

app = Flask(__name__)

def connect_db():
    conn = sqlite3.connect("notes.db")
    return conn

def database_connection(function):
    def wrap():
        with connect_db() as conn:
            cursor = conn.cursor()
            res = function(cursor)
            conn.commit()
        return res
    wrap.__name__ = function.__name__
    return wrap

def get_handwriting(img_bytes):
    headers = {'Content-Type': 'application/octet-stream', 'Ocp-Apim-Subscription-Key': '4edb8729e3ef4a5cb18b46ca5f5fdcaf'}
    url = "https://westeurope.api.cognitive.microsoft.com/vision/v1.0/recognizeText"
    r = requests.post(url, data=img_bytes, headers=headers)
    if r.status_code != 202:
        raise Exception("Bad response from recognizeText: {}".format(r.status_code))
    operation_location = r.headers["Operation-Location"]
    # allow prcoessing time
    time.sleep(3)
    headers = {'Ocp-Apim-Subscription-Key': '4edb8729e3ef4a5cb18b46ca5f5fdcaf'}
    r = requests.get(operation_location, headers=headers)
    if r.status_code != 200:
        raise Exception("Bad status code from operation location: {}".format(r.status_code))
    return r.content

def cv_image_from_b64_str(b64str):
    im_bytes = base64.b64decode(b64str)
    np_arr = np.fromstring(decoded_str, np.uint8)
    return cv2.imdecode(np_arr, cv2.CV_LOAD_IMAGE_COLOR)

@app.route("/")
def index():
    return "Hello World"

@app.route("/createnote", methods=["POST"])
@database_connection
def create_note():
    img = cv_image_from_b64_st(request.form["image"])
    name = request.form["name"]
    orb = cv2.ORB_Create()

    _, des = orb.computeAndDetect(img, None)
    des_p = cpickle.dumps(des)
    img_p = cpickle.dumps(img)


@app.route("/initdb")
@database_connection
def init_db(cursor):
    delete_sql = "DROP TABLE notes;"
    table_sql = "CREATE TABLE notes (name TEXT PRIMARY KEY, pickled_des TEXT, pickled_img TEXT);"
    try:
        cursor.execute(delete_sql)
    except:
        pass
    cursor.execute(table_sql)
    return "OK"

@app.route("/findnote", methods=["POST"])
@database_connection
def find_note(cursor):
    # init ORB and bfmatcher (eventually FLANN)
    orb = cv2.ORB_create()
    bf = cv2.BFMatcher()
    # computeanddetect (descriptors)
    _, des = orb.computeAndDetect(img, None)
    # fetch all previous des from database
    # match for each and compute #good
    # return id with most good
    pass

if __name__ == "__main__":
    app.run(debug=True)
