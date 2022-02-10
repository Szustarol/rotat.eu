#!/usr/bin/python3

import tensorflow.keras as keras
import tensorflow as tf

def build_simple_model():
    model_input = keras.layers.Input(shape = (32, ))
    model_dense = keras.layers.Dense(10, activation="relu")(model_input)
    model_output_1 = keras.layers.Dense(1, activation="sigmoid")(model_dense)
    #model_output_2 = keras.layers.Dense(1, activation="sigmoid")(model_dense)

    #return keras.models.Model(model_input, [model_output_1, model_output_2])
    return keras.models.Model(model_input, model_output_1)

if __name__ == "__main__":
    model = build_simple_model()
    model.summary()
    model.compile(
            optimizer=keras.optimizers.Nadam(),
            metrics=["accuracy"],
            loss="mse"
    )
    model.save("simple_model.h5")
