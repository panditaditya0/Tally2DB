package com.tally_backup.tally_backup.Dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class Entry {
    @XmlElement(name = "F01")
    private String f01;

    @XmlElement(name = "F02")
    private String f02;

    @XmlElement(name = "F03")
    private double f03;

    @XmlElement(name = "F04")
    private double f04;

    @XmlElement(name = "F05")
    private String f05;

    @XmlElement(name = "F06")
    private int f06;

    @XmlElement(name = "F07")
    private int f07;

    @XmlElement(name = "F08")
    private int f08;

    @XmlElement(name = "F09")
    private String f09;

    @XmlElement(name = "F10")
    private int f10;

    @XmlElement(name = "F11")
    private int f11;

    @XmlElement(name = "F12")
    private int f12;

    @XmlElement(name = "F13")
    private String f13;

}
