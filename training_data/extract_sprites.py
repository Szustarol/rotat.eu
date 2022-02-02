#!/usr/bin/python3
from ipaddress import collapse_addresses
from turtle import width
from PIL import Image, ImageOps
from multiprocessing.sharedctypes import Value
import os
import subprocess
import cv2
import numpy as np
import shutil
import matplotlib.pyplot as plt
import pickle
os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3' 
import tensorflow as tf

source_dir = 'spritesets'
output_name = 'images'

output_height = 128
output_width = 64

def get_spriteset_dimensions(spriteset_file):
    """
        Input: image file path
        Output: number of rows and columns in the image
    """
    pimage = Image.open(spriteset_file)
    pimage = ImageOps.grayscale(pimage)
    gimage = np.array(pimage)
    gimage = cv2.resize(gimage, (gimage.shape[1]*2, gimage.shape[0]*2), interpolation=cv2.INTER_NEAREST)
    background_color = gimage[0, 0]
    # Replace all color of this type with 0
    gimage[gimage == background_color] = 0
    # Now binarise it with some small threshold
    gimage[gimage > 0] = 1

    # Erosion of size 3 to get rid of touching boundaries
    gimage = cv2.morphologyEx(gimage, cv2.MORPH_CLOSE, np.ones((3, 3)))
    
    trials_remaining = 25


    def calculate_nonzero_intervals(array):
        entries_detected = 0
        entry_detected = False
        last_distance = None
        last_position = None
        for position, entry in enumerate(array):
            if not entry:
                if last_position != None:
                    new_distance = position-last_position
                    if last_distance != None:
                        abs_diff = abs(new_distance-last_distance)/last_distance
                        if abs_diff > 0.4:
                            return -1
                    last_distance = new_distance
                    last_position = None
                entry_detected = False
            elif not entry_detected:
                entries_detected += 1
                entry_detected = True
                last_position = position
        return entries_detected


    while trials_remaining > 0:
        gimage = cv2.erode(gimage, np.ones((7, 7)), iterations=1)
        col_sums = np.sum(gimage, axis = 0)
        cols_detected = calculate_nonzero_intervals(col_sums)

        row_sums = np.sum(gimage, axis = 1)
        row_sums[row_sums < cols_detected*2] = 0
        rows_detected = calculate_nonzero_intervals(row_sums)
        

        if rows_detected % 4 != 0 or rows_detected == 0 or cols_detected < 0:
            trials_remaining-=1
            if np.sum(gimage) == 0:
                trials_remaining = 0
        else:
            break            
    else:
        if rows_detected > 0:
            raise ValueError(f"Number of rows should be divisble by four and nonzero, got {rows_detected}")
        else:
            raise ValueError("Unable to separate consecutive rows or cols")

    return (rows_detected, cols_detected)

def get_image_slices(image, rows, cols):
    """
        Groups the image by four rows
    """
    pimage = Image.open(image).convert('RGB')
    gimage = np.array(pimage)
    height, width, _ = gimage.shape
    outputs = []
    for col in range(0, cols):
        col_start = col*width//cols
        col_end = (col+1)*width//cols
        for row_start in range(0, rows, 4):
            current_slices = []
            for row_current in range(row_start, row_start+4):
                row_start = row_current*height//rows
                row_end = (row_current+1)*height//rows
                fragment = gimage[row_start:row_end,col_start:col_end,:]
                fragment = cv2.resize(fragment, (output_width, output_height), cv2.INTER_NEAREST)
                fragment = fragment/255
                current_slices.append(tf.constant(fragment))
            outputs.append(tf.stack(current_slices))
    del pimage
    del gimage
    del fragment
    return outputs




if __name__ == "__main__":
    print("Sprite extraction started")
    dirent = [os.path.join(source_dir, f) for f in os.listdir(source_dir)]
    dirent = [f for f in dirent if os.path.isfile(f)]
    print(f"Found {len(dirent)} spritesets.")
    valid_images = {}
    for image_source in dirent:
        print(image_source)
        try:
            rows, cols = get_spriteset_dimensions(image_source)
            print(f"\tFound {rows} rows and {cols} cols.")
            valid_images[image_source] = (rows, cols)
        except Exception as e:
            print(f"\tCannot parse image: {e}.",)

        
    print(f"{len(valid_images)} out of {len(dirent)} images passed.")

    slices = []

    for image, (rows, cols) in valid_images.items():
        slices.extend(get_image_slices(image, rows, cols))
    
    print(f"Acquired {len(slices)} samples total")

    # for show_sample in range(5):
    #     sample_slice = slices[80+show_sample]
    #     for sample_index in range(4):        
    #         plt.subplot(5, 4, show_sample*4 + sample_index+1)
    #         plt.imshow(sample_slice[sample_index])
    # plt.show()

    dataset = tf.data.Dataset.from_tensor_slices(slices)

    try:
        shutil.rmtree('loaded_data')
    except Exception as e:
        print(f'Tree not removed: {e}.')

    tf.data.experimental.save(
        dataset, 'loaded_data', 'GZIP'
    )

    with open('loaded_data/element_spec', 'wb') as es_out:
        pickle.dump(dataset.element_spec, es_out)