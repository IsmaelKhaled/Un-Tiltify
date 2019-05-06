from flask import Flask, request, Response
import os
import base64
from algorithm import image_aligner
import cv2
import json
import logging
from logging.handlers import RotatingFileHandler
app = Flask(__name__)
folder = './'
app.config['UPLOAD_FOLDER'] = folder
@app.route("/", methods=['POST'])
def hello():
    image = request.form['img']
    image = base64.b64decode(image)
    image_result = open('capturedphoto.jpg','wb')
    image_result.write(image)
    app.logger.info("Image received and saved!")
    #image.save(os.path.join(app.config['UPLOAD_FOLDER'], "capturedphoto.jpg"))
    print(app.config['UPLOAD_FOLDER'])
    img = cv2.imread('capturedphoto.jpg', 1)
    new_image = image_aligner(img)
    cv2.imwrite(os.path.join(app.config['UPLOAD_FOLDER'] + "/output.jpg"), cv2.cvtColor(new_image, cv2.COLOR_RGB2BGR))
    output = open('output.jpg','rb')
    output_read = output.read()
    image_64_encode = base64.b64encode(output_read).decode('utf-8')
    response = {}
    response['img'] = image_64_encode
    json_response = json.dumps(response)

    return json_response

handler = RotatingFileHandler('foo.log', maxBytes=10000, backupCount=1)
handler.setLevel(logging.DEBUG)
app.logger.addHandler(handler)
app.logger.setLevel(logging.DEBUG)
app.run(host= '0.0.0.0')
