from flask import Flask, jsonify, request
import adafruit_dht
import board
import time
from tflite_runtime.interpreter import Interpreter
import cv2
import numpy as np
from flask_cors import CORS
import base64
import io

app = Flask(__name__)
CORS(app)

classes = [
    "Apple___Apple_scab", "Apple___Black_rot", "Apple___Cedar_apple_rust", "Apple___healthy",
    "Blueberry___healthy", "Cherry_(including_sour)___Powdery_mildew", "Cherry_(including_sour)___healthy",
    "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot", "Corn_(maize)___Common_rust_", "Corn_(maize)___Northern_Leaf_Blight",
    "Corn_(maize)___healthy", "Grape___Black_rot", "Grape___Esca_(Black_Measles)", "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)",
    "Grape___healthy", "Orange___Haunglongbing_(Citrus_greening)", "Peach___Bacterial_spot", "Peach___healthy",
    "Pepper,_bell___Bacterial_spot", "Pepper,_bell___healthy", "Potato___Early_blight", "Potato___Late_blight",
    "Potato___healthy", "Raspberry___healthy", "Soybean___healthy", "Squash___Powdery_mildew", "Strawberry___Leaf_scorch",
    "Strawberry___healthy", "Tomato___Bacterial_spot", "Tomato___Early_blight", "Tomato___Late_blight", "Tomato___Leaf_Mold",
    "Tomato___Septoria_leaf_spot", "Tomato___Spider_mites Two-spotted_spider_mite", "Tomato___Target_Spot",
    "Tomato___Tomato_Yellow_Leaf_Curl_Virus", "Tomato___Tomato_mosaic_virus", "Tomato___healthy"
]

dht11 = adafruit_dht.DHT11(board.D4)
model_path = "/home/raspi/projet/model.tflite"
interpreter = Interpreter(model_path=model_path)
interpreter.allocate_tensors()

@app.route('/data', methods=['GET'])
def get_data():
    try:
        temperature = dht11.temperature
        humidity = dht11.humidity
        if temperature is None or humidity is None:
            return jsonify({"error": "Impossible de lire les donnees du capteur DHT11"}), 400

        image_path = "/home/raspi/projet/feuille.jpg"
        image = cv2.imread(image_path)
        if image is None:
            return jsonify({"error": f"Erreur de lecture de l image a l emplacement {image_path}"}), 400

        image = cv2.resize(image, (128, 128))
        image = np.expand_dims(image, axis=0).astype(np.float32) / 255.0
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        interpreter.set_tensor(input_details[0]['index'], image)
        interpreter.invoke()
        result = interpreter.get_tensor(output_details[0]['index'])
        result = result.tolist()

        if not result or len(result[0]) == 0:
            return jsonify({"error": "Le modele n a pas retourne de resultats valides"}), 400
        
        predicted_class = classes[np.argmax(result)]
        return jsonify({
            "temperature": temperature,
            "humidity": humidity,
            "predicted_class": predicted_class
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
