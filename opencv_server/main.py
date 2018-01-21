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
    print("Base64 str end {}".format(b64str[-7:]))
    b64str_ = b64str + '==='
    return base64.b64decode(b64str_.encode("utf-8"), '-_')

def cv_image_from_str(str_):
    np_arr = np.fromstring(str_, np.uint8)
    return cv2.imdecode(np_arr, cv2.CV_LOAD_IMAGE_COLOR)

@database_connection
def fetch_des_db(cursor):
    cursor.execute("SELECT name, pickled_des, annotations FROM notes;")
    return [(name, cPickle.loads(s), ann) for (name, s, ann) in cursor.fetchall()]

def compute_num_good(des1, des2):
    bf = cv2.BFMatcher()
    matches = bf.knnMatch(des1, des2, k=2)
    num_good = 0
    for m, n in matches:
        if m.distance < 0.75*n.distance:
            num_good += 1
    return num_good

@app.route("/createnote", methods=["POST"])
@database_connection
def create_note(cursor):
    str_img = str_from_b64(request.form["image"])
    img = cv_image_from_str(str_img)
    name = request.form["name"]
    orb = orb_f()

    _, des = orb.detectAndCompute(img, None)
    des_p = cPickle.dumps(des)
    if 'b' in des_p:
        idx = des_p.index('b')
        print(des_p[idx-4:idx+4])
    print name
    cursor.execute('INSERT INTO notes VALUES ("' + name + '", ?, ?, "");', [des_p, str_img])
    return "OK"

@app.route("/initdb")
@database_connection
def init_db(cursor):
    delete_sql = "DROP TABLE notes;"
    table_sql = "CREATE TABLE notes (name TEXT PRIMARY KEY, pickled_des TEXT, image BLOB, annotations TEXT);"
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
    orb = orb_f()
    bf = cv2.BFMatcher()
    # computeanddetect (descriptors)
    img = cv_image_from_str(str_from_b64(request.form["image"]))
    _, des = orb.detectAndCompute(img, None)
    # fetch all previous des from database
    des_list = fetch_des_db()
    # match for each and compute #good
    max_good = -1
    goodest_name = ""
    goodest_annotations = ""
    for name, des_db, annotations in des_list:
        num_good = compute_num_good(des, des_db)
        if num_good > max_good:
            max_good = num_good
            goodest_name = name
            goodest_annotations = annotations
    print "{}: {}".format(goodest_name, max_good)
    return goodest_name + ":" + goodest_annotations

@app.route("/addannotation/<noteid>/<annotation>")
def add_annotation(noteid, annotation):
        with connect_db() as conn:
            cursor = conn.cursor()
            cursor.execute("UPDATE notes SET annotations=? WHERE name=?;", [annotation, noteid])
            conn.commit()
        return "OK"

if __name__ == "__main__":
    app.run(debug=True, host='0.0.0.0', port=5001)
