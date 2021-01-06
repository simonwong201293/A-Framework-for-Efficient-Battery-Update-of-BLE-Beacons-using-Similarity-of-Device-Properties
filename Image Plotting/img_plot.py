import pandas as pd
import matplotlib.pyplot as plt
path1 = "C:\\Users\\sw-admin\\Desktop\\新增資料夾\\beacons_report_203233f178b1_09072020_est.csv"
path2 = "C:\\Users\\sw-admin\\Desktop\\新增資料夾\\beacons_report_203233f178b1_09072020_raw.csv"

headers1 = ['BeaconId', 'Estimated Voltage', 'Time']
data1 = pd.read_csv(path1)
ids = data1['BeaconId'].unique().tolist()
print(ids)
for id in ids:
    x = []
    y = []
    for row in data1.iterrows():
        # print(row[1]['BeaconId'])
        if row[1]['BeaconId'] == id:
            y.append(row[1][' Estimated Voltage'])
            x.append(row[1][' Time'])
            plt.plot(x, y)
plt.legend(loc="upper left")
plt.show()
data2 = pd.read_csv(path2)
ids = data2['BeaconId'].unique().tolist()
print(ids)
headers2 = ['BeaconId', 'Raw Voltage', 'Time']
for id in ids:
    x = []
    y = []
    for row in data2.iterrows():
        # print(row[1]['BeaconId'])
        if row[1]['BeaconId'] == id and (row[1][' Time'] % 60 == 0):
            y.append(row[1][' Raw Voltage'])
            x.append(row[1][' Time'])
            plt.plot(x, y)
plt.legend(loc="upper left")
plt.show()