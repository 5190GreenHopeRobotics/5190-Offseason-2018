import matplotlib.pyplot as plt
import numpy as np
import math
from matplotlib import animation
from networktables import NetworkTables

NetworkTables.initialize(server='10.51.90.2')


# ALL UNITS IN INCHES
def drawField(plax):
    # Calculate field length and width in inches
    fieldDim = [12 * 27, 12 * 54]

    # Draw individual elements
    fieldBoundary = [(29.69, 0), (fieldDim[0] - 29.69, 0), (fieldDim[0], 35.0), (fieldDim[0], fieldDim[1] - 35.0),
                     (fieldDim[0] - 29.69, fieldDim[1]), (29.69, fieldDim[1]), (0, fieldDim[1] - 35.0), (0, 35.0),
                     (29.69, 0)]
    plax.plot([p[0] for p in fieldBoundary], [p[1] for p in fieldBoundary], color="black")

    switchClose = [(85.25, 140), (fieldDim[0] - 85.25, 140), (fieldDim[0] - 85.25, 196), (85.25, 196), (85.25, 140)]
    plax.plot([p[0] for p in switchClose], [p[1] for p in switchClose], color="red")

    switchFar = [(85.25, fieldDim[1] - 140), (fieldDim[0] - 85.25, fieldDim[1] - 140),
                 (fieldDim[0] - 85.25, fieldDim[1] - 196), (85.25, fieldDim[1] - 196), (85.25, fieldDim[1] - 140)]
    plax.plot([p[0] for p in switchFar], [p[1] for p in switchFar], color="blue")

    scale = [(71.57, 299.65), (fieldDim[0] - 71.57, 299.65), (fieldDim[0] - 71.57, 348.35), (71.57, 348.35),
             (71.57, 299.65)]
    plax.plot([p[0] for p in scale], [p[1] for p in scale], color="black")

    platform = [(95.25, 261.47), (fieldDim[0] - 95.25, 261.47), (fieldDim[0] - 95.25, 386.53), (95.25, 386.53),
                (95.25, 261.47)]
    plax.plot([p[0] for p in platform], [p[1] for p in platform], color="black")

    nullZoneLeft = [(0.0, 288.0), (95.25, 288.0), (95.25, 288.0 + 72.0), (0, 288.0 + 72.0)]
    plax.plot([p[0] for p in nullZoneLeft], [p[1] for p in nullZoneLeft], linestyle=':', color="black")

    nullZoneRight = [(fieldDim[0], 288.0), (fieldDim[0] - 95.25, 288.0), (fieldDim[0] - 95.25, 288.0 + 72.0),
                     (fieldDim[0], 288.0 + 72.0)]
    plax.plot([p[0] for p in nullZoneRight], [p[1] for p in nullZoneRight], linestyle=':', color="black")

    autoLineClose = [(0, 120.0), (fieldDim[0], 120.0)]
    plax.plot([p[0] for p in autoLineClose], [p[1] for p in autoLineClose], linestyle=':', color="black")

    autoLineFar = [(0, fieldDim[1] - 120.0), (fieldDim[0], fieldDim[1] - 120.0)]
    plax.plot([p[0] for p in autoLineFar], [p[1] for p in autoLineFar], linestyle=':', color="black")

    exchangeZoneClose = [(149.69, 0), (149.69, 36.0), (149.69 - 48.0, 36.0), (149.69 - 48.0, 0)]
    plax.plot([p[0] for p in exchangeZoneClose], [p[1] for p in exchangeZoneClose], linestyle=':', color="red")

    exchangeZoneFar = [(fieldDim[0] - 149.69, fieldDim[1]), (fieldDim[0] - 149.69, fieldDim[1] - 36.0),
                       (fieldDim[0] - 149.69 + 48.0, fieldDim[1] - 36.0), (fieldDim[0] - 149.69 + 48.0, fieldDim[1])]
    plax.plot([p[0] for p in exchangeZoneFar], [p[1] for p in exchangeZoneFar], linestyle=':', color="black")

    powerCubeZoneClose = [(161.69 - 22.5, 140), (161.69 - 22.5, 140 - 42.0), (161.69 + 22.5, 140 - 42.0),
                          (161.69 + 22.5, 140)]
    plax.plot([p[0] for p in powerCubeZoneClose], [p[1] for p in powerCubeZoneClose], linestyle=':', color="green")

    powerCubeZoneFar = [(161.69 - 22.5, fieldDim[1] - 140), (161.69 - 22.5, fieldDim[1] - 140 + 42.0),
                        (161.69 + 22.5, fieldDim[1] - 140 + 42.0), (161.69 + 22.5, fieldDim[1] - 140)]
    plax.plot([p[0] for p in powerCubeZoneFar], [p[1] for p in powerCubeZoneFar], linestyle=':', color="green")


def rotatePoint(p, center, angle):
    s = math.sin(math.radians(angle))
    c = math.cos(math.radians(angle))

    px = p[0] - center[0]
    py = p[1] - center[1]
    pxn = px * c - py * s
    pyn = px * s + py * c

    px = pxn + center[0]
    py = pyn + center[1]
    return px, py


def genRobotSquare(p, heading):
    robotWidth = 27.0
    robotLength = 33.0

    topLeft = (p[0] - robotWidth / 2.0, p[1] + robotLength / 2.0)
    topRight = (p[0] + robotWidth / 2.0, p[1] + robotLength / 2.0)
    bottomLeft = (p[0] - robotWidth / 2.0, p[1] - robotLength / 2.0)
    bottomRight = (p[0] + robotWidth / 2.0, p[1] - robotLength / 2.0)

    topLeft = rotatePoint(topLeft, p, heading)
    topRight = rotatePoint(topRight, p, heading)
    bottomLeft = rotatePoint(bottomLeft, p, heading)
    bottomRight = rotatePoint(bottomRight, p, heading)

    boxArr = [topLeft, topRight, bottomRight, bottomLeft, topLeft]
    return boxArr


xdata = [0.0]
ydata = [0.0]
headings = [0.0]

# Path
pxdata = [0.0]
pydata = [0.0]
pheadings = [0.0]


def updatePoint(point, pathpoint, robot, actualPath, targetPath):
    ntinstance = NetworkTables.getTable('PosePlotter')

    if not ntinstance.getBoolean('Is Auto', False):
        return

    if ntinstance.getBoolean('ResetPlot', False):
        del xdata[:]
        del ydata[:]
        del headings[:]
        del pxdata[:]
        del pydata[:]
        del pheadings[:]
        ntinstance.putBoolean('ResetPlot', False)

    xval = ntinstance.getNumber('Robot X', 0.0)
    yval = ntinstance.getNumber('Robot Y', 0.0)
    hval = ntinstance.getNumber('Robot Heading', 0.0)

    if xval > .01 or yval > .01:
        xdata.append(xval)
        ydata.append(yval)
        headings.append(hval)

    pxval = ntinstance.getNumber('Path X', 0.0)
    pyval = ntinstance.getNumber('Path Y', 0.0)
    phval = ntinstance.getNumber('Path Heading', 0.0)

    # Try to avoid "teleporting" when observer is reset.
    if pxval > .01 or pyval > .01:
        pxdata.append(pxval)
        pydata.append(pyval)
        pheadings.append(phval)

    point.set_data(np.array([xval, yval]))
    pathpoint.set_data(np.array([pxval, pyval]))
    robotData = genRobotSquare((xval, yval), hval)
    robot.set_data([p[0] for p in robotData], [p[1] for p in robotData])
    actualPath.set_data(xdata, ydata)
    targetPath.set_data(pxdata, pydata)
    return [point, pathpoint, robot, actualPath, targetPath]


# Generate the figure and draw static elements
fig = plt.figure()
ax = fig.add_subplot(111, aspect='equal')
drawField(ax)
targetPath, = ax.plot(pxdata, pydata, color='red', alpha=0.5)
actualPath, = ax.plot(xdata, ydata, color='black', alpha=0.25)

pathpoint, = ax.plot(pxdata[0], pydata[0], marker='o', markersize=5, color="red")
point, = ax.plot(xdata[0], ydata[0], marker='o', markersize=5, color="blue")
startingRobot = genRobotSquare((xdata[0], ydata[0]), 0.0)
robot, = ax.plot([p[0] for p in startingRobot], [p[1] for p in startingRobot], color="black")

plt.margins(x=0.1, y=0.1)

ani = animation.FuncAnimation(fig, updatePoint, len(xdata), fargs=(point, pathpoint, robot, actualPath, targetPath))
plt.show()
