import torch
from torch import nn


class Classifier(nn.Module):
    def __init__(self, input_size, hidden_size, output_size, dropout):
        super(Classifier, self).__init__()

        self.input_size = input_size
        self.hidden_size = hidden_size
        self.num_layers = 1
        self.output_size = output_size
        self.num_directions = 1  # 单向LSTM
        self.dropout = dropout

        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'

        self.lstm = nn.LSTM(self.input_size, self.hidden_size, self.num_layers, batch_first=True, dropout=self.dropout)
        self.linear = nn.Linear(self.hidden_size, self.output_size)

    def forward(self, x):
        batch_size, seq_len = x.shape[0], x.shape[1]

        h_0 = torch.randn(self.num_directions * self.num_layers, batch_size, self.hidden_size).to(self.device)
        c_0 = torch.randn(self.num_directions * self.num_layers, batch_size, self.hidden_size).to(self.device)

        output, _ = self.lstm(x, (h_0, c_0))
        pred = self.linear(output)
        pred = pred[:, -1, :]
        return pred


class MyLSTM(nn.Module):
    def __init__(self):
        super().__init__()
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'

        self.num_layers = 1
        self.num_directions = 1  # 单向LSTM
        self.hidden_size = 64

        self.lstm = nn.LSTM(input_size=128, hidden_size=64, num_layers=self.num_layers, batch_first=True)
        self.dropout = nn.Dropout(p=0.2)
        self.linear = nn.Linear(64, 5)
        self.act = nn.Sigmoid()

    def forward(self, x):
        batch_size, seq_len = x.shape[0], x.shape[1]

        h_0 = torch.randn(self.num_directions * self.num_layers, batch_size, self.hidden_size).requires_grad_().to(
            self.device)
        c_0 = torch.randn(self.num_directions * self.num_layers, batch_size, self.hidden_size).requires_grad_().to(
            self.device)

        output, _ = self.lstm(x, (h_0.detach(), c_0.detach()))
        output = self.dropout(output)
        pred = self.linear(output[:, -1, :])
        pred = self.act(pred)
        return output[:, -1, :], pred
