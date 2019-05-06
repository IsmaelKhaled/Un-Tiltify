import cv2 as cv
import math
import numpy as np
import matplotlib
import matplotlib.pyplot as plt



def plot_side_by_side(first, second, input_name, output_name, img_type):
    f, (ax1, ax2) = plt.subplots(1, 2, figsize=(20, 25))
    if img_type == 0:
        # grey
        ax1.imshow(cv.cvtColor(first, cv.COLOR_GRAY2RGB),interpolation="bilinear")
        ax2.imshow(cv.cvtColor(second, cv.COLOR_GRAY2RGB),interpolation="bilinear")
    else:
        ax1.imshow(cv.cvtColor(first, cv.COLOR_BGR2RGB),interpolation="bilinear")
        ax2.imshow(cv.cvtColor(second, cv.COLOR_BGR2RGB),interpolation="bilinear")
    ax1.set_title(input_name)
    ax2.set_title(output_name)
    plt.show()
    
def horiz_line(angle,portrait):
    if(portrait):
        if ((angle > 178 and angle <182) or (angle >-2 and angle<2)):
            return True
        else:
            return False
    else:
        if (angle > 88 and angle <92):
            return True
        else:
            return False

def angle180(angle,portrait):
    if(portrait):
        if ((angle > 150 and angle <210) or (angle >-30 and angle<30)):
            return True
        else:
            return False
    else:
        if (angle > 60 and angle <120):
            return True
        else:
            return False
def angle90(angle,portrait):
    if(portrait):
        if ((angle > 60 and angle <120)):
            return True
        else:
            return False
    else:
        if ((angle > 150 and angle <210) or (angle >-30 and angle<30)):
            return True
        else:
            return False



def image_aligner(normal_BGR):
    font = cv.FONT_HERSHEY_SIMPLEX
    img = cv.cvtColor(normal_BGR, cv.COLOR_BGR2RGB) #Transform from BGR to RGB
    gray = cv.cvtColor(img, cv.COLOR_RGB2GRAY) #Make a greyscale copy of the image
    kernel_ellipse_3x3 = cv.getStructuringElement(cv.MORPH_ELLIPSE, (9, 9))
    opened = cv.morphologyEx(gray, cv.MORPH_OPEN, kernel_ellipse_3x3) #Open the greyscale copy to remove some detail from the image
    edges = cv.Canny(opened, 50, 150, apertureSize=3) #Canny detection to detect the lines
    lines = cv.HoughLines(edges, 1, np.pi/180, 150) #Hough transform to detect more robust lines from the canny edge detection
    
    
    edges = np.float32(edges)
    dst = cv.cornerHarris(edges,2,3,0.04)
    
    #Detect if the image is shot in portrait or landscape mode
    if(img.shape[1] < img.shape[0]):
        portrait = True
    else:
        portrait = False
    
    #Detect the far top corner to be used for reference lines
    for x in range(dst.shape[0]):
        for y in range(dst.shape[1]):
            if dst[x,y] > 0:
                top_corner = x
                top_corner_y = y
                break
        if dst[x,y] > 0:
            break
    for i in range(-3,3):
        img[top_corner+i,:] = [0,255,0]
    test = np.zeros_like(img)
    
    #Detect the far bottom corner to be used for reference lines
    for x in range(dst.shape[0]):
        for y in range(dst.shape[1]):
            if dst[x,y] > 0:
                bot_corner = x
                bot_corner_y = y
    for i in range(-3,3):
        img[bot_corner+i,:] = [0,255,0]
    
    #Initialize angles arrays
    angles90 = []
    angles180 = []
    coords_90 = []
    coords_180 = []
    
    
    #Initialize margin values to the far right for the minimum and far left for the maximum
    min_X = img.shape[1]-1
    max_X = 0
    
    
    #Calculate the Xs and Ys of all the Hough lines detected to calculate the tilt angle later on
    for line in lines:
        for rho, theta in line:
            a = np.cos(theta)
            b = np.sin(theta)
            x0 = a*rho
            y0 = b*rho
            x1 = int(x0 + 1000*(-b))
            y1 = int(y0 + 1000*(a))
            x2 = int(x0 - 1000*(-b))
            y2 = int(y0 - 1000*(a))
            theta = theta*180/np.pi
            
            #Update the margin X values as the lines get closer to the image
            if (horiz_line(theta,portrait) and abs(x1) < img.shape[1] and x1 < min_X):
                min_X = x1 if x1>0 else x1+img.shape[1]
            elif (horiz_line(theta,portrait) and abs(x1) < img.shape[1] and x1 > max_X):
                max_X = x1 if x1>0 else x1+img.shape[1]
                
            if (angle90(theta,portrait)):
                angles90.append(theta)
            elif (angle180(theta,portrait)):
                angles180.append(theta)
            #cv.line(img,(x1,y1),(x2,y2),(0,0,255),2)
    
            
    #Make the margin lines and calculate the margin values
    for i in range(-3,3):
        if max_X+1 < img.shape[1] and min_X+1 < img.shape[1]:
            img[:,min_X+i] = [0,255,0]
            img[:,max_X+i] = [0,255,0]
        
    margin_left = min_X
    margin_right = img.shape[1] - max_X
    
    
    #Calculate the tilt angle of the photo
    array90 = np.array(angles90)
    array180 = np.array(angles180)
    average90 = np.average(array90)
    average180 = np.average(array180)
    
    
    #Print the margins on the photo if the photo is upright (angles are close to 0)
    #if (average90 < 92 and average90 > 88 and portrait) or ((not portrait) and average180 < 92 and average180 > 88):
    if min_X != img.shape[1]-1:
        cv.putText(img,str(margin_left),(int(min_X/2),int(img.shape[0]/2)), font, 6,(0,0,255),2,cv.LINE_AA)
    if max_X != 0:
        cv.putText(img,str(margin_right),(int(max_X-margin_right/16),int(img.shape[0]/2)), font, 6,(0,0,255),2,cv.LINE_AA)
        
    #Print the difference between the angle and the reference line above the reference line.
    if not portrait:
        cv.putText(img,str(int(90-average180)),(int(img.shape[1]/2),top_corner), font, 6,(0,0,255),2,cv.LINE_AA)
        cv.putText(img,"o",(int((img.shape[1]/2)+len(str(int(90-average180)))*100),top_corner-80), font, 3,(0,0,255),2,cv.LINE_AA)
    else:
        cv.putText(img,str(int(90-average90)),(int(img.shape[1]/2),top_corner), font, 6,(0,0,255),2,cv.LINE_AA)
        cv.putText(img,"o",(int((img.shape[1]/2)+len(str(int(90-average90)))*100),top_corner-80), font, 3,(0,0,255),2,cv.LINE_AA) 
		
    return img

#Plot the result (Only for testing)
# plot_side_by_side(img, dst, 'image', 'Canny',1)
