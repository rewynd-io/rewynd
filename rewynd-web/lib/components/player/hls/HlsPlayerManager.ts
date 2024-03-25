/* eslint @typescript-eslint/no-unsafe-declaration-merging: 0 */

import {
  HlsStreamProps,
  Progress,
  StreamStatus,
} from "@rewynd.io/rewynd-client-typescript";
import Hls, { ErrorData, Events, FragLoadedData } from "hls.js";
import { Duration, Second } from "durr";
import EventEmitter from "events";
import { HttpClient } from "../../../const";
import { WebLog } from "../../../log";
import { MediaSelection } from "./HlsPlayer";

const log = WebLog.getChildCategory("HlsPlayerManager");

export declare interface HlsPlayerManagerEvents {
  readonly next: (manager: HlsPlayerManager) => void;
  readonly load: (streamProps: HlsStreamProps) => void;
}

export declare interface HlsPlayerManager extends EventEmitter {
  on<U extends keyof HlsPlayerManagerEvents>(
    event: U,
    listener: HlsPlayerManagerEvents[U],
  ): this;

  emit<U extends keyof HlsPlayerManagerEvents>(
    event: U,
    ...args: Parameters<HlsPlayerManagerEvents[U]>
  ): boolean;
}

export class HlsPlayerManager extends EventEmitter {
  streamProps?: HlsStreamProps;
  private heartbeatTimer: ReturnType<typeof setInterval> | undefined;
  streamStatus: StreamStatus | undefined;
  available: number | undefined;
  private element: HTMLVideoElement | undefined;
  private lastProgress: Promise<Progress | undefined> =
    Promise.resolve(undefined);
  private awaitingNext = false;
  // eslint-disable-next-line @typescript-eslint/ban-ts-comment
  // @ts-ignore
  private _audioContext: AudioContext; // keep the tab active even between media.
  lastMediaSelection?: MediaSelection;

  constructor(
    private readonly hls = new Hls({
      debug: true,
      maxBufferHole: 2.5,
      maxFragLookUpTolerance: 0.5,
      startPosition: 0,
      renderTextTracksNatively: true,
    }),
  ) {
    super();
    this.hls.subtitleDisplay = true;
    this._audioContext = new AudioContext({});
  }

  async load(selection: MediaSelection) {
    log.info("Loading", selection);
    await this.unload();
    this.lastMediaSelection = selection;
    this.streamProps = await HttpClient.createStream({
      createStreamRequest: selection.request,
    });
    return new Promise<void>((resolve) => {
      const triggerHeartbeat = (interval: Duration) => {
        if (this.heartbeatTimer) {
          clearInterval(this.heartbeatTimer);
          this.heartbeatTimer = undefined;
        }
        this.heartbeatTimer = setInterval(async () => {
          if (this.streamProps) {
            const latestStatus = await HttpClient.heartbeatStream({
              streamId: this.streamProps.id,
            });
            log.info(`Stream ${this.streamProps.id} Status: `, latestStatus);
            const priorStatus = this.streamStatus;
            this.streamStatus = latestStatus;
            if (latestStatus == StreamStatus.Canceled) {
              await this.load({
                info: selection.info,
                request: {
                  ...selection.request,
                  startOffset:
                    ((await this.lastProgress)?.percent ?? 0) *
                    (this.streamProps.duration ?? 0),
                },
              });
              resolve();
            } else if (latestStatus == StreamStatus.Available) {
              if (priorStatus != StreamStatus.Available) {
                triggerHeartbeat(Duration.seconds(15));
                this.setupNudge();
                this.hls.loadSource(this.streamProps.url);
                this.emit("load", this.streamProps);
                this.awaitingNext = false;
                resolve();
              } else {
                this.putUserProgress();
              }
            }
          }
        }, interval.millis);
      };

      triggerHeartbeat(Duration.milliseconds(500));
    });
  }

  /*
  Sometimes HLS.js won't autoplay after loading a source the first time.
  This nudges it after 0.5 seconds to get it to start playing if it hasn't already
   */
  private setupNudge() {
    const handler = (_event: Events.FRAG_LOADED, data: FragLoadedData) => {
      if (data.frag.sn == 0) {
        this.hls.off(Events.FRAG_LOADED, handler);
        const nudge = () => {
          const current = this.currentTime;
          setTimeout(() => {
            if (current == this.currentTime) {
              console.log("Stuck after loading, nudging hls to start");
              if (this.element) {
                this.element.fastSeek(current + 0.01);
              }
            }
          }, Second.toMillis(0.5));
        };
        nudge();
      }
    };
    this.hls.on(Events.FRAG_LOADED, handler);
  }

  async unload(stop = false) {
    log.debug(`Unloading`);
    if (stop) {
      this.hls.stopLoad();
    }
    if (this.streamProps) {
      await HttpClient.deleteStream({ streamId: this.streamProps.id });
      this.streamProps = undefined;
    }
    this.streamStatus = undefined;
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
    }
    this.available = undefined;
  }

  private hlsErrorHandler = async (event: Events.ERROR, data: ErrorData) => {
    log.error("Hls.js error", event, data);
  };

  bind(element: HTMLVideoElement) {
    this.unbind();
    this.element = element;
    this.hls.detachMedia();
    this.hls.attachMedia(this.element);
    this.hls.on(Hls.Events.ERROR, this.hlsErrorHandler);
    element.addEventListener("durationchange", () => {
      this.available = element.duration;
    });
    element.addEventListener("timeupdate", async () => {
      this.putUserProgress();
      if (
        this.streamProps &&
        !this.awaitingNext &&
        element.currentTime >=
          this.streamProps.duration - this.streamProps.startOffset - 0.1
      ) {
        this.awaitingNext = true;
        log.info("Emitting next event");
        this.emit("next", this); // TODO only emit once
      }
    });
  }

  unbind() {
    log.debug("Unbinding");
    this.hls.off(Hls.Events.ERROR, this.hlsErrorHandler);
    this.hls.detachMedia();
    // TODO removeEventListeners
    this.element = undefined;
  }

  async seek(desiredTimestamp: number) {
    log.info("Seeking", desiredTimestamp);

    if (
      this.lastMediaSelection &&
      this.streamProps &&
      this.available &&
      this.element
    ) {
      if (
        desiredTimestamp < this.streamProps.startOffset ||
        desiredTimestamp > this.available + this.streamProps.startOffset
      ) {
        await this.load({
          ...this.lastMediaSelection,
          request: {
            ...this.lastMediaSelection.request,
            startOffset: desiredTimestamp,
          },
          // TODO subtitles
        });
      } else {
        this.element.currentTime =
          desiredTimestamp - this.streamProps.startOffset;
        const paused = this.element.paused;
        await this.element.play();
        if (paused) {
          this.element.pause();
        }
        this.emit("load", this.streamProps);
      }
    }
  }

  public get duration(): number {
    return this.streamProps?.duration ?? 0;
  }

  public get currentTime(): number {
    const val =
      (this.hls?.media?.currentTime ?? this.element?.currentTime ?? 0) +
      (this.streamProps?.startOffset ?? 0);
    log.info(`CurrentTime: ${val}`);
    return val;
  }

  public putUserProgress() {
    this.lastProgress = this.lastProgress.then(async (last) => {
      const currTime =
        this.hls?.media?.currentTime ?? this.element?.currentTime;
      if (currTime && this.streamProps && this.lastMediaSelection) {
        const currPct =
          (currTime + this.streamProps.startOffset) / this.streamProps.duration;
        const pctDiff = Math.abs((last?.percent ?? 0) - currPct);
        const timeDiff = pctDiff * this.streamProps.duration;
        if (
          !last || // Write progress only if it differs by more than 10 seconds
          last.id != this.lastMediaSelection.info.id ||
          timeDiff > 10
        ) {
          const progress: Progress = {
            id: this.lastMediaSelection.info.id,
            percent: currPct,
            timestamp: new Date(),
          };
          await HttpClient.putUserProgress({ progress: progress });
          return progress;
        }
      }
      return last;
    });
  }
}
