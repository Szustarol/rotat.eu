#!/usr/bin/python3

from operator import index
import tensorflow as tf
import tensorflow.keras as keras

def build_model_v2(model_image_width=64, model_image_height=128):
    """Loosely based on the U-net architecture"""
    encoder_input = keras.layers.Input(shape = ( model_image_height, model_image_width, 3,))
    conv_input = encoder_input
    conv_input = keras.layers.BatchNormalization()(conv_input)
    n_filters = [16, 16, 32, 32, 64, 64, 128, 128, 256, 256, 512, 512]
    prev_filter = n_filters[1]
    conv_layers = []
    for filter_size in n_filters:
        if filter_size != prev_filter:
            conv_input = keras.layers.MaxPooling2D((2, 2), padding='same')(conv_input)
        conv_input = keras.layers.Conv2D(filter_size, (3, 3), activation='relu', padding='same')(conv_input)
        conv_layers.append(conv_input)
        conv_input = keras.layers.BatchNormalization()(conv_input)
        prev_filter = filter_size
    conv_layers.reverse()
    conv_input = keras.layers.MaxPooling2D((2, 2), padding='same')(conv_input)

    def build_path(entry):
        nonlocal n_filters, conv_layers
        n_filters_rev = list(reversed(n_filters))
        prev_filter = n_filters_rev[0]
        for idx, filter in enumerate(n_filters_rev):
            if filter != prev_filter:
                entry = keras.layers.UpSampling2D((2, 2))(entry)
            entry = keras.layers.Conv2D(filter, (3, 3), activation='relu', padding='same')(entry)
            if idx > 1:
                entry = keras.layers.concatenate([entry, conv_layers[idx-2]])
            entry = keras.layers.BatchNormalization()(entry)
            prev_filter = filter
        entry = keras.layers.UpSampling2D((2, 2))(entry)
        entry = keras.layers.Conv2D(3, (3, 3), activation='sigmoid', padding='same')(entry)
        return entry

    left_rotation = build_path(conv_input)
    right_rotation = build_path(conv_input)
    back_rotation = build_path(conv_input)
    

    return keras.models.Model(encoder_input, [left_rotation, right_rotation, back_rotation])

def build_model(model_image_width = 64, model_image_height = 128):
    encoder_input = keras.layers.Input(shape = ( model_image_height, model_image_width, 3,))
    conv_input = encoder_input
    n_filters = [16, 32, 64, 128, 256]
    for filter_size in n_filters:
        conv_input = keras.layers.Conv2D(filter_size, (3, 3), activation='relu', padding='same')(conv_input)
        conv_input = keras.layers.MaxPooling2D((2, 2), padding='same')(conv_input)


    # now split the model into three paths for rotation

    def build_path(entry):
        n_filters = [256, 128, 64, 32, 16]
        for filter in n_filters:
            entry = keras.layers.Conv2D(filter, (3, 3), activation='relu', padding='same')(entry)
            entry = keras.layers.UpSampling2D((2, 2))(entry)
        entry = keras.layers.Conv2D(3, (3, 3), activation='sigmoid', padding='same')(entry)
        return entry
    
    left_rotation = build_path(conv_input)
    right_rotation = build_path(conv_input)
    back_rotation = build_path(conv_input)
    

    return keras.models.Model(encoder_input, [left_rotation, right_rotation, back_rotation])