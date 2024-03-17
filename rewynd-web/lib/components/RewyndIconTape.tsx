import React from "react";
import RewyndIoIconBasicSvg from "../static/img/RewyndIcon-Basic.svg";
import RewyndIoIconTapeSvg from "../static/img/RewyndIcon-Tape.svg";
export type RewyndIconProps = Omit<
  React.DetailedHTMLProps<
    React.ImgHTMLAttributes<HTMLImageElement>,
    HTMLImageElement
  >,
  keyof { src: string; alt: string }
>;

export const RewyndIconTape = (props: RewyndIconProps) => (
  <img {...props} src={RewyndIoIconTapeSvg} alt={"Rewynd Icon Tape"} />
);

export const RewyndIconBasic = (props: RewyndIconProps) => (
  <img {...props} src={RewyndIoIconBasicSvg} alt={"Rewynd Icon Basic"} />
);
