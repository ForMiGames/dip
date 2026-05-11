package com.example.telecomsim.service.compression;

import com.example.telecomsim.model.channel.ChannelParameters;
import com.example.telecomsim.model.channel.ChannelType;
import com.example.telecomsim.model.metrics.TransmissionResult;
import com.example.telecomsim.model.simulation.TransmissionProtocol;

public interface ChannelSimulator {
    TransmissionResult simulate(
            byte[] data,
            ChannelParameters params,
            TransmissionProtocol protocol
    );
    ChannelType getSupportedChannelType();
}
