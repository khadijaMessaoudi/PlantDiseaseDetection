
import time
import adafruit_dht
import board
# Configuration du capteur DHT11
dht11 = adafruit_dht.DHT11(board.D4)  # GPIO4 (broche physique 7)
print("Lecture des done du capteur dht11")
while True:
  try:
     temperature=dht11.temperature
     humidity= dht11.humidity
     print(f"Temperature : {temperature:.1f} C")
     print(f"Humidite:{humidity:.1f}%")
  except RuntimeError as error:
     print(f"Erreur de lecture:{error.args[0]}")
  time.sleep(2)
