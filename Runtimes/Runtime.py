import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
import plotly.express as px
import plotly.io as pio

from svglib.svglib import svg2rlg
from reportlab.graphics import renderPDF
import plotly.graph_objects as go

tools =  {
     "RefactoringMiner 2.0":"RMiner2_analyzed.txt"
    , "RefactoringMiner 1.0":"RMiner1_analyzed.txt"
    , "RefDiff 2.0":"Runtime2x_analyzed.txt"
    ,  "RefDiff 0.1.1":"Runtime011_analyzed.txt"
    , "RefDiff 1.0":"Runtime1x_analyzed.txt"
    }

#layout = go.Layout (yaxis = dict(type = 'linear', autorange= True))
fig = go.Figure()
c = 0
for tool in tools:
    data = pd.read_csv(tools[tool])
    print(tool)
    if tool ==  "RefactoringMiner 2.0":
        print(data.sort_values(" Runtime"))  
    print("Mean = ",data[" Runtime"].mean())
    print("Median = ",data[" Runtime"].median())
    print("Max = ",data[" Runtime"].max())
    fig.add_trace(go.Violin(name=tool,y=data[' Runtime'],x = data['Tool'],
                            box_visible=True,
                            meanline_visible=True, fillcolor='gray', line_color='black', showlegend=False, spanmode="hard", orientation = "v", scalemode="count"))

    fig.add_trace(go.Scatter(x = [tool], y = [data[" Runtime"].mean()], text =["Mean = "+"{:6.2f}".format(data[" Runtime"].mean())]
                            , textposition="top right", mode = "text", textfont = dict(family="Rockwell", size = 15, color='black')
                            , showlegend=False))
    fig.add_trace(go.Scatter(x = [tool], y = [data[" Runtime"].median()], text =["Median = "+ str(data[" Runtime"].median())]
                            , textposition="top right", mode = "text", textfont = dict(family="Rockwell", size = 15, color='black')
                            , showlegend=False))
    fig.add_trace(go.Scatter(x = [tool], y = [data[" Runtime"].max()], text =["Max = "+ str(data[" Runtime"].max())]
                            , textposition="top right", mode = "text", textfont = dict(family="Rockwell", size = 15, color='black')
                            , showlegend=False))
    c = c +1
    

# fig.update_layout(title="Execution time comparison of Refactoring Mining Tools",font=dict(size=35, family='Rockwell', color='black'))
fig.update_yaxes(title_font=dict(size=30, family='Rockwell', color='black'),type='log', range= (0,5.5), tickfont=dict(family='Rockwell', color='black', size=20))
fig.update_xaxes(title_font=dict(size=30, family='Rockwell', color='black'), tickfont=dict(family='Rockwell', color='black', size=20))

fig.write_image("D:/MyProjects/TSEAnalysis/Evaluation/runtimes.pdf",width=1500, height=800)
# pio.to_image(fig,"SVG")
fig.show()




