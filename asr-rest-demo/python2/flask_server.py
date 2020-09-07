from flask import Flask, request
import logging,json
logging.getLogger().setLevel(logging.DEBUG)

app = Flask(__name__)
# logging.basicConfig(level=logging.DEBUG,
#                     filename='res-client.log',
#                     filemode='a',
#                     format=
#                     '%(asctime)s - %(pathname)s[line:%(lineno)d] - %(levelname)s: %(message)s'
#                     )
@app.route("/res", methods=["GET","POST"])
def add():
    res = ObjectToDict(request.values)
    logging.info(res)
    word = res["data"][0]["word"]
    logging.info(word)
    return res

def ObjectToDict(object):
    for i in object:
        return eval(i)

if __name__ == '__main__':
    app.run(port=50001)
