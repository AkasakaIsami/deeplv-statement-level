import configparser
import os

import pandas as pd
import torch
from gensim.models import Word2Vec

from dataset import MyDataset

import warnings

from eval import train_and_test

warnings.filterwarnings("ignore")


def dictionary_and_embedding(raw_dir, project, embedding_size):
    """
    :param project: 输入要计算embedding的项目
    :param embedding_size: 要训练的词嵌入大小
    """
    corpus_file_path = os.path.join(raw_dir, 'data.txt')
    model_file_name = project + "_w2v_" + str(embedding_size) + '.model'

    save_path = os.path.join(raw_dir, model_file_name)
    if os.path.exists(save_path):
        return

    from gensim.models import word2vec

    corpus = word2vec.LineSentence(corpus_file_path)
    w2v = word2vec.Word2Vec(corpus, vector_size=embedding_size, workers=16, sg=1, min_count=3)
    w2v.save(save_path)


def preprocess_data(raw_dir: str, process_dir: str, project: str, embedding_dim):
    '''
    逻辑是 先看process_dir里存没存
    没存再处理
    '''
    # 先导入embedding矩阵
    word2vec_path = os.path.join(raw_dir, project + '_w2v_' + str(embedding_dim) + '.model')
    word2vec = Word2Vec.load(word2vec_path).wv
    embeddings = torch.from_numpy(word2vec.vectors)
    embeddings = torch.cat([embeddings, torch.zeros(1, embedding_dim)], dim=0)

    def word2vector(word: str):
        max_token = word2vec.vectors.shape[0]
        index = [word2vec.key_to_index[word] if word in word2vec.key_to_index else max_token]
        return embeddings[index]

    data_file_name = os.path.join(process_dir, 'data.pkl')
    if os.path.exists(data_file_name):
        return

    datalist = pd.DataFrame(columns=['id', 'data', 'label'])

    with open(os.path.join(raw_dir, 'data.txt'), 'r') as file:
        for line in file:
            id = line.split(' ', 1)[0]

            label = int(id.split('@', 1)[0])

            if label == 0:
                label = torch.tensor([[1, 0, 0, 0, 0]])
            elif label == 1:
                label = torch.tensor([[1, 1, 0, 0, 0]])
            elif label == 2:
                label = torch.tensor([[1, 1, 1, 0, 0]])
            elif label == 3:
                label = torch.tensor([[1, 1, 1, 1, 0]])
            elif label == 4:
                label = torch.tensor([[1, 1, 1, 1, 1]])
            else:
                continue

            id = id.split('@', 1)[1]

            words = line.split(' ')[1:]

            vectors = torch.randn(0, embedding_dim)
            for word in words:
                vector = word2vector(word)
                vectors = torch.cat([vectors, vector], dim=0)

            datalist.loc[len(datalist)] = [id, vectors, label]

    os.makedirs(process_dir)
    datalist = datalist.sample(frac=1)
    datalist.to_pickle(os.path.join(process_dir, 'data.pkl'))


def make_dataset(process_dir: str):
    datalist_file_path = os.path.join(process_dir, 'data.pkl')

    if not (os.path.exists(datalist_file_path)):
        print(f'缺少文件{datalist_file_path}')
        return

    datalist = pd.read_pickle(datalist_file_path)
    dataset = MyDataset(datalist)

    return dataset


if __name__ == '__main__':
    # 读取配置
    cf = configparser.ConfigParser()
    cf.read('config.ini')

    project = cf.get('data', 'projectName')
    ratio = cf.get('data', 'ratio')
    p_n_ratio = cf.getint('sample', 'PosNegRatio')
    p_increase_rate = cf.getfloat('sample', 'PosIncreaseRate')

    raw_dir = os.path.join(cf.get('data', 'dataDir'), project, 'raw')
    process_dir = os.path.join(cf.get('data', 'dataDir'), project, 'process')

    embedding_dim = cf.getint('embedding', 'dim')

    print(f'开始数据预处理（目标项目为{project}）...')
    print('step1: 词嵌入训练...')
    dictionary_and_embedding(raw_dir, project, embedding_dim)

    print('step2: 处理原始数据...')
    preprocess_data(raw_dir, process_dir, project, embedding_dim)

    print('step3: 制作数据集...')
    dataset = make_dataset(process_dir)

    print('step4: 开始训练和测试...')
    train_and_test(dataset)
