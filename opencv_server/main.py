from flask import Flask, request
import sqlite3
import requests

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

@app.route("/")
def index():
    return "Hello World"

@app.route("/features", methods=["POST"])
def get_features():
    # assume that request has base64 encoded jpg
    b64str = request.form["image"]
    return b64str

@app.route("/initdb")
@database_connection
def init_db(cursor):
    delete_sql = "DROP TABLE notes;"
    table_sql = "CREATE TABLE notes (name TEXT PRIMARY KEY);"
    try:
        cursor.execute(delete_sql)
    except:
        pass
    cursor.execute(table_sql)
    return "OK"

@app.route("/createnote", methods=["POST"])
@database_connection
def create_note(cursor):
    b64img = request.form["image"]
    cursor.execute("INSERT INTO notes VALUES ('" + b64img + "');")
    return "OK"

if __name__ == "__main__":
    app.run(debug=True)
