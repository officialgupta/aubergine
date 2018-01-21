from flask import Flask, request
import base64
import sqlite3
import requests
import cv2
import numpy as np
import cPickle

app = Flask(__name__)
orb_f = None
try:
    orb_f = cv2.ORB
except AttributeError:
    orb_f = cv2.ORB_create

def connect_db():
    conn = sqlite3.connect("notes.db")
    conn.text_factory = str
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

def str_from_b64(b64str):
    b64str_ = b64str + ('=' * (-len(b64str) % 4))
    return base64.b64decode(b64str_)

def cv_image_from_str(str_):
    np_arr = np.fromstring(str_, np.uint8)
    return cv2.imdecode(np_arr, cv2.CV_LOAD_IMAGE_COLOR)

@app.route("/createnote", methods=["POST"])
@database_connection
def create_note(cursor):
    str_img = str_from_b64(request.form["image"])
    img = cv_image_from_str(str_img)
    name = request.form["name"]
    orb = orb_f()

    _, des = orb.detectAndCompute(img, None)
    des_p = cPickle.dumps(des)
    cursor.execute("INSERT INTO notes VALUES ('" + name + "', '" + des_p + "', ?);", [str_img])
    return "OK"

@app.route("/initdb")
@database_connection
def init_db(cursor):
    delete_sql = "DROP TABLE notes;"
    table_sql = "CREATE TABLE notes (name TEXT PRIMARY KEY, pickled_des TEXT, image BLOB);"
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
    _, des = orb.detectAndCompute(img, None)
    # fetch all previous des from database
    # match for each and compute #good
    # return id with most good
    pass

if __name__ == "__main__":
    app.run(debug=True, host='0.0.0.0', port=5001)
