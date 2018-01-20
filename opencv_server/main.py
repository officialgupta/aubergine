from flask import Flask, request
import sqlite3

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

@app.route("/")
def index():
    return "Hello World"

@app.route("/features", methods=["POST"])
def get_features():
    # assume that request has base64 encoded jpg
    b64str = request.form["image"]
    # calculate the index based on the image - Cameron
    # send back the name, index, associations

    json = { name: "Hello", link: "blup.com" }

    return json

@app.route("/initdb")
@database_connection
def init_db(cursor):
    delete_sql = "DROP TABLE notes;"
    table_sql = "CREATE TABLE notes (name TEXT PRIMARY KEY, image BLOB);"
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
    note_name = request.form["name"]
    cursor.execute("INSERT INTO notes VALUES ('" + note_name + ',' + b64img + "');")
    return "OK"

if __name__ == "__main__":
    app.run(debug=True, host='0.0.0.0', port=5001)
