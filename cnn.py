import cv2
import numpy as np
from tflite_runtime.interpreter import Interpreter
diseases = [
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
model_path = '/home/raspi/projet/model.tflite'
try:
    interpreter = Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
except Exception as e:
     print (f" erreur lors du chargement du model:{e}")
     exit()
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()
image_path = '/home/raspi/projet/feuille.jpg'
image = cv2.imread(image_path)
if image is None:
     print (f" erreur: impossible de charger l'image a l'emplacement{image_path}")
     exit()

input_shape = input_details[0]['shape']  # Exemple : [1, 128, 128, 3]
height, width = input_shape[1], input_shape[2]
image_resized = cv2.resize(image, (width, height))
image_resized = image_resized.astype(np.float32) / 255.0
input_data = np.expand_dims(image_resized, axis=0)
if input_data.shape != tuple(input_shape):
    print (f" erreur : Les dimensions de l'image redimensionnee {input_data.shape}"f"ne correspondent pas aux dimensions attendues par le modele{input_shape}.")
    exit()


try:
    interpreter.set_tensor(input_details[0]['index'], input_data)
    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details[0]['index'])

    if len(output_data.shape) != 2 or output_data.shape[1] != len(diseases):
       print("Erreur : Les dimensions de sortie du modelene correspondent pas au nombre de classes.")
       exit()
    result = np.argmax(output_data)
    predicted_disease = diseases[result]
    print(f"La feuille est malade : {predicted_disease}")
except Exception as e:
       print(f"Erreur lors de la prediction : {e}")
       exit()     
